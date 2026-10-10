# Quickstart: Validating Opt-in Release Signing

Run from the repository root (or a scratchpad copy with the patches applied). **No step publishes to Maven Central.** Commands that could reach a registry are only in the temporary-copy procedure and the maintainer-only sections.

## Agent checks (during implement)

### A1. Profile separation

```bash
mvn -q help:effective-pom -Doutput=/tmp/eff-default.xml
mvn -q -Prelease help:effective-pom -Doutput=/tmp/eff-release.xml
mvn help:active-profiles
mvn -Prelease help:active-profiles
diff /tmp/eff-default.xml /tmp/eff-release.xml
```

(Write outputs in the scratchpad directory in practice.) Expected: `maven-gpg-plugin` and `central-publishing-maven-plugin` appear only in the `-Prelease` output; `release` is listed as active only with `-Prelease`.

### A2. Credential-free build

```bash
mvn clean verify
find target -name '*.asc'
```

Expected: build passes with no prompt; JaCoCo gate passes; `find` prints nothing; sources and javadoc jars exist.

### A3. Publication hold property (inspection only)

```bash
mvn -q -Prelease help:effective-pom -Doutput=<scratchpad>/a.xml
mvn -q -Prelease -Dcentral.autoPublish=true help:effective-pom -Doutput=<scratchpad>/b.xml
grep -n autoPublish <scratchpad>/a.xml <scratchpad>/b.xml
```

Expected: `false` in `a.xml`, `true` in `b.xml`. Only `help:effective-pom` runs; nothing is uploaded.

### A4. Unactivated release deploy fails (US3, temporary copy only)

```bash
TMP=$(mktemp -d)            # inside the scratchpad directory
cp -r . "$TMP/proj" && cd "$TMP/proj"
mvn -q versions:set -DnewVersion=9.9.9 -DgenerateBackupPoms=false
: > "$TMP/empty-settings.xml"   # replace by a minimal <settings/> file via Write
mvn -s "$TMP/empty-settings.xml" -DskipTests deploy
```

Expected: build fails with Maven's native "repository element was not specified" error, before any upload (the message does not mention `-Prelease`; accepted trade-off Q8). The copy has no credentials and no `release` profile, so Central is unreachable. Delete the copy afterwards. Never run `versions:set` in the working tree.

## Maintainer checks

### M1. Signing (own terminal, pinentry)

```bash
export GPG_TTY=$(tty)
mvn -Prelease clean verify
for f in target/*.asc; do gpg --verify "$f"; done
```

Expected: pinentry prompt appears; `.asc` exists for jar, sources and javadoc; every `gpg --verify` reports a good signature. `verify` does not upload.

### M2. First real release

Release per the "Cutting a release" section in `CONTRIBUTING.md` with default settings. Confirm in the Central Portal that the bundle is validated and held for manual publication before approving.

### M3. Optional snapshot deploy

With GitHub Packages credentials in your own `settings.xml`, deploy a `-SNAPSHOT` version without `-Prelease` and confirm it reaches GitHub Packages as before.