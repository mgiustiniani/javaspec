package io.github.jvmspec.compatibility.language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LanguageCoverageManifest {
    static final String RESOURCE = "/java-language-matrix/coverage.tsv";
    private static final String HEADER = "profile\tconstruct\tdisposition\tevidence\tscenario\tfixture\tdescription";

    private LanguageCoverageManifest() {
    }

    static List<LanguageCoverageEntry> load() throws IOException {
        InputStream stream = LanguageCoverageManifest.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IOException("Missing language coverage manifest: " + RESOURCE);
        }
        List<LanguageCoverageEntry> entries = new ArrayList<LanguageCoverageEntry>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        try {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new IOException("Unexpected language coverage manifest header: " + header);
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 7) {
                    throw new IOException("Manifest line " + lineNumber + " must contain 7 tab-separated fields, found " + fields.length);
                }
                try {
                    entries.add(new LanguageCoverageEntry(
                            fields[0],
                            fields[1],
                            LanguageCoverageEntry.Disposition.valueOf(fields[2]),
                            LanguageCoverageEntry.EvidenceStatus.valueOf(fields[3]),
                            fields[4],
                            fields[5],
                            fields[6]
                    ));
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Invalid manifest line " + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
        } finally {
            reader.close();
        }
        return Collections.unmodifiableList(entries);
    }
}
