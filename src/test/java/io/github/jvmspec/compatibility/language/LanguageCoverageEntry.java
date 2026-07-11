package io.github.jvmspec.compatibility.language;

import java.util.Objects;

final class LanguageCoverageEntry {
    enum Disposition {
        GENERATED,
        UPDATED,
        PRESERVED,
        PROFILE_GATED,
        INTENTIONALLY_UNSUPPORTED
    }

    enum EvidenceStatus {
        COVERED,
        PLANNED
    }

    private final String profile;
    private final String constructId;
    private final Disposition disposition;
    private final EvidenceStatus evidenceStatus;
    private final String scenario;
    private final String fixturePath;
    private final String description;

    LanguageCoverageEntry(
            String profile,
            String constructId,
            Disposition disposition,
            EvidenceStatus evidenceStatus,
            String scenario,
            String fixturePath,
            String description
    ) {
        this.profile = required(profile, "profile");
        this.constructId = required(constructId, "constructId");
        this.disposition = Objects.requireNonNull(disposition, "disposition must not be null");
        this.evidenceStatus = Objects.requireNonNull(evidenceStatus, "evidenceStatus must not be null");
        this.scenario = required(scenario, "scenario");
        this.fixturePath = fixturePath == null ? "" : fixturePath;
        this.description = required(description, "description");
    }

    String profile() {
        return profile;
    }

    String constructId() {
        return constructId;
    }

    Disposition disposition() {
        return disposition;
    }

    EvidenceStatus evidenceStatus() {
        return evidenceStatus;
    }

    String scenario() {
        return scenario;
    }

    String fixturePath() {
        return fixturePath;
    }

    String description() {
        return description;
    }

    boolean hasFixture() {
        return fixturePath.length() > 0 && !"-".equals(fixturePath);
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
