package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import io.camunda.zeebe.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Handles {@link Participant} elements, their associated {@link Process},
 * and all {@link Lane} entries (which become {@link Stage} objects).
 *
 * <p>Populates: workflowName, workflowId, workflowVersion, workflowDocumentation,
 * processType, processSubtype, and the stages list in {@link ParsingContext}.</p>
 */
public class ParticipantHandler implements ElementHandler {

    private static final Logger LOGGER = Logger.getLogger(ParticipantHandler.class.getName());

    @Override
    public void handle(ParsingContext ctx) {
        ModelElementType participantType =
                ctx.modelInstance.getModel().getType(Participant.class);
        Collection<ModelElementInstance> participants =
                ctx.modelInstance.getModelElementsByType(participantType);

        if (participants.isEmpty()) {
            if (ctx.bpmnProperties.getParticipant("presence").isRequired()) {
                ctx.addInconsistency(PARTICIPANT_REQUIRED.of());
            }
            return;
        }

        List<Participant> all = participants.stream()
                .map(p -> (Participant) p)
                .toList();
        List<Participant> withProcess = all.stream()
                .filter(p -> p.getProcess() != null)
                .toList();

        if (withProcess.isEmpty()) {
            // No pool references a process: validate every participant as before
            // (each one may report PROCESS_REQUIRED).
            all.forEach(p -> handleParticipant(p, ctx));
            return;
        }

        // Black-box pools (external participants without a process) are ignored.
        // The workflow header and stages come from the first pool with a process;
        // previously every participant overwrote the header and the last one won.
        Participant main = withProcess.get(0);
        if (withProcess.size() > 1) {
            LOGGER.warning(() -> String.format(
                    "BpmnFlow supports one process per model, but %d participants reference a process."
                            + " Workflow header and stages are taken from '%s'; ignored: %s.",
                    withProcess.size(), main.getId(),
                    withProcess.subList(1, withProcess.size()).stream()
                            .map(Participant::getId).toList()));
        }
        handleParticipant(main, ctx);
    }

    private void handleParticipant(Participant participant, ParsingContext ctx) {
        ctx.workflowName = participant.getName();

        if (ctx.bpmnProperties.getParticipant("name").isRequired()
                && isBlank(ctx.workflowName)) {
            ctx.addInconsistency(PARTICIPANT_NAME_MISSING.of(participant.getId()));
        }

        Process process = participant.getProcess();
        if (process == null) {
            if (ctx.bpmnProperties.getProcess("presence").isRequired()) {
                ctx.addInconsistency(PROCESS_REQUIRED.of(participant.getId()));
            }
            return;
        }
        handleProcess(process, participant, ctx);
    }

    private void handleProcess(Process process, Participant participant, ParsingContext ctx) {
        ctx.workflowId = process.getId();
        if (ctx.bpmnProperties.getProcess("id").isRequired() && isBlank(ctx.workflowId)) {
            ctx.addInconsistency(PROCESS_ID_MISSING.of(participant.getId()));
        }

        // Previously: process.getCamundaVersionTag() — C7-only API, removed.
        // Now: the adapter knows where to look (C7=camunda: attribute, C8=zeebe:versionTag element).
        ctx.workflowVersion = ctx.engineAdapter.extractVersionTag(process);
        if (ctx.bpmnProperties.getProcess("versionTag").isRequired()
                && isBlank(ctx.workflowVersion)) {
            ctx.addInconsistency(PROCESS_VERSION_MISSING.of(participant.getId()));
        }

        handleDocumentation(process, participant, ctx);
        handleExtensionProperties(process, participant, ctx);
        handleLanes(process, ctx);
    }

    private void handleDocumentation(Process process, Participant participant,
                                     ParsingContext ctx) {
        Collection<Documentation> docs = process.getDocumentations();
        if (docs.isEmpty()) {
            if (ctx.bpmnProperties.getProcess("documentation").isRequired()) {
                ctx.addInconsistency(DOCUMENTATION_REQUIRED.of(participant.getId()));
            }
            return;
        }
        for (Documentation doc : docs) {
            ctx.workflowDocumentation = doc.getTextContent();
            if (ctx.bpmnProperties.getProcess("documentation").isRequired()
                    && isBlank(ctx.workflowDocumentation)) {
                ctx.addInconsistency(PROCESS_DOCUMENTATION_MISSING.of(participant.getId()));
            }
        }
    }

    private void handleExtensionProperties(Process process, Participant participant,
                                           ParsingContext ctx) {
        Map<String, String> attrs = AttributeExtractor.extract(process, ctx.engineAdapter);
        attrs.putAll(AttributeExtractor.extract(participant, ctx.engineAdapter));

        ctx.processType = attrs.get("process_type");
        if (ctx.bpmnProperties.getParticipant("process_type").isRequired()
                && isBlank(ctx.processType)) {
            ctx.addInconsistency(PROCESS_TYPE_MISSING.of(participant.getId()));
        }

        ctx.processSubtype = attrs.get("process_subtype");
        if (ctx.bpmnProperties.getParticipant("process_subtype").isRequired()
                && isBlank(ctx.processSubtype)) {
            ctx.addInconsistency(PROCESS_SUBTYPE_MISSING.of(participant.getId()));
        }
    }

    private void handleLanes(Process process, ParsingContext ctx) {
        Collection<LaneSet> laneSets = process.getLaneSets();
        if (laneSets.isEmpty()) {
            if (ctx.bpmnProperties.getLane("presence").isRequired()) {
                ctx.addInconsistency(LANE_REQUIRED.of());
            }
            return;
        }
        for (LaneSet laneSet : laneSets) {
            for (Lane lane : laneSet.getLanes()) {
                handleLane(lane, ctx);
            }
        }
    }

    private void handleLane(Lane lane, ParsingContext ctx) {
        String stageCode = AttributeExtractor.extractOne(lane, "stage", ctx.engineAdapter);
        if (ctx.bpmnProperties.getLane("stage").isRequired() && isBlank(stageCode)) {
            ctx.addInconsistency(LANE_STAGE_MISSING.of(lane.getId()));
        }

        String laneName = lane.getName();
        if (isBlank(laneName)) {
            if (ctx.bpmnProperties.getLane("name").isRequired()) {
                ctx.addInconsistency(LANE_NAME_MISSING.of(lane.getId()));
            }
        } else {
            ctx.addStage(new Stage(laneName, stageCode));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
