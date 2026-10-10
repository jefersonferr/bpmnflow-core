# Quickstart: Validating Opt-in Release Signing

Agent checks run inside the local Git clone `$SCRATCH/work` (see `tasks.md` T001), where `$SCRATCH` is the session scratchpad directory; never in the real working tree. Maintainer checks run in the real working tree after the patch is applied. **No step publishes to Maven Central.** Commands that could reach a registry are only in the temporary-copy procedure and the maintainer-only sections.

## Agent checks (during implement)

### A1. Profile separation

```bash
cd $SCRATCH/work
mvn -q help:effective-pom -Doutput=$SCRATCH/eff-default.xml
mvn -q -Prelease help:effective-pom -Doutput=$SCRATCH/eff-release.xml
mvn help:active-profiles
mvn -Prelease help:active-profiles
diff $SCRATCH/eff-default.xml $SCRATCH/eff-release.xml
```

Expected: `maven-gpg-plugin` and `central-publishing-maven-plugin` appear only in the `-Prelease` output; `release` is listed as active only with `-Prelease`.

### A2. Credential-free build

```bash
cd $SCRATCH/work
mvn clean verify
find target -name '*.asc'
mvn clean verify -Dgpg.skip
```

Expected: build passes with no prompt; JaCoCo gate passes; `find` prints nothing; sources and javadoc jars exist; the old `-Dgpg.skip` flag is harmless and the build still passes.

### A3. Publication hold property (inspection only)

```bash
cd $SCRATCH/work
mvn -q -Prelease help:effective-pom -Doutput=$SCRATCH/a.xml
mvn -q -Prelease -Dcentral.autoPublish=true help:effective-pom -Doutput=$SCRATCH/b.xml
grep -n autoPublish $SCRATCH/a.xml $SCRATCH/b.xml
grep -n "maven.pkg.github.com\|central.sonatype.com" $SCRATCH/eff-default.xml
```

Expected: `false` in `a.xml`, `true` in `b.xml`. In the default effective POM (`eff-default.xml`) the GitHub Packages `snapshotRepository` is present and the Central `<repository>` is absent (FR-008). Only `help:effective-pom` runs; nothing is uploaded.

### A4. Unactivated release deploy fails (US3, temporary copy only)

```bash
cp -r $SCRATCH/work $SCRATCH/us3-tmp && cd $SCRATCH/us3-tmp
mvn -q versions:set -DnewVersion=9.9.9 -DgenerateBackupPoms=false
# create $SCRATCH/empty-settings.xml with the Write tool, containing only <settings/>
mvn -s $SCRATCH/empty-settings.xml -DskipTests deploy
```

Expected: build fails with Maven's native "repository element was not specified" error, before any upload (the message does not mention `-Prelease`; accepted trade-off Q8). The copy has no credentials and no `release` profile, so Central is unreachable. Delete the copy afterwards. Never run `versions:set` in the real working tree.

## Maintainer checks

Run in this order (matches `tasks.md` T020–T026):

1. Apply `patches/001-release-signing-profile.patch` through IntelliJ.
2. `mvn clean verify` in the real working tree: passes, no prompt, no `.asc` in `target/`.
3. M1 (below).
4. Commit.
5. Open the pull request and confirm CI is green.
6. M2 at the first real release.
7. M3 (optional).

### M1. Signing (own terminal, pinentry)

```bash
export GPG_TTY=$(tty)
mvn -Prelease clean verify
for f in target/*.asc; do gpg --verify "$f"; done
```

Expected: pinentry prompt appears; `.asc` exists for jar, sources and javadoc; every `gpg --verify` reports a good signature. `verify` does not upload. Optional: with no signing key available, the build fails at the signing step, before any upload.

### M2. First real release

Release per the "Cutting a release" section in `CONTRIBUTING.md` with default settings. Confirm in the Central Portal that the bundle is validated and held for manual publication before approving.

### M3. Optional snapshot deploy

With GitHub Packages credentials in your own `settings.xml`, deploy a `-SNAPSHOT` version without `-Prelease` and confirm it reaches GitHub Packages as before.