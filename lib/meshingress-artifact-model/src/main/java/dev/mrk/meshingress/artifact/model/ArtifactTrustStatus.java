package dev.mrk.meshingress.artifact.model;

public enum ArtifactTrustStatus {
    RECEIVED(false),
    QUARANTINED(false),
    SCANNING(false),
    REVIEW_PENDING(false),
    APPROVED_TRUSTED(true),
    APPROVED_LIMITED(true),
    REJECTED(false),
    BLOCKED_MALWARE(false),
    BLOCKED_POLICY(false),
    BLOCKED_VULNERABILITY(false),
    SUPERSEDED(false),
    REVOKED(false);

    private final boolean installable;

    ArtifactTrustStatus(boolean installable) {
        this.installable = installable;
    }

    public boolean installable() {
        return installable;
    }
}
