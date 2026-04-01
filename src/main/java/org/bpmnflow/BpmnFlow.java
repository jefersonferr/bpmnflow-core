package org.bpmnflow;

import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.ModelParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BpmnFlow {
    public static void main(String[] args) {
        // Validate arguments before accessing them
        if (args == null || args.length < 2) {
            System.err.println("Usage: java -jar bpmn-model-parser.jar <modelPath> <externalConfigPath>");
            System.exit(1); // Exit with error code
        }

        String modelPath = args[0];
        String externalConfigPath = args[1];

        // Validate that the file paths are not null or empty
        if (modelPath == null || modelPath.isBlank() || externalConfigPath == null || externalConfigPath.isBlank()) {
            System.err.println("Error: Model path and external config path must not be empty.");
            System.exit(1);
        }

        // Read model file safely using try-with-resources
        try (InputStream modelStream = getModelStreamFromFile(modelPath)) {
            Workflow workflow = ModelParser.parser(modelStream, externalConfigPath);
            System.out.println("Model: " + workflow);
        } catch (IOException e) {
            System.err.println("Error reading model file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static InputStream getModelStreamFromFile(String modelPath) throws IOException {
        Path path = Path.of(modelPath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IOException("File not found or cannot be read: " + modelPath);
        }
        return Files.newInputStream(path);
    }
}
