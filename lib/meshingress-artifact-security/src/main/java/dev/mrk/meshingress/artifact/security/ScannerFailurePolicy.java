package dev.mrk.meshingress.artifact.security;

public enum ScannerFailurePolicy {
    BLOCK,
    REVIEW,
    IGNORE;

    public ScannerStatus scannerStatusForFailure() {
        return switch (this) {
            case BLOCK -> ScannerStatus.BLOCKED;
            case REVIEW -> ScannerStatus.REVIEW;
            case IGNORE -> ScannerStatus.FAILED;
        };
    }

    public static ScannerFailurePolicy fromWire(String value, ScannerFailurePolicy fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? BLOCK : fallback;
        }
        try {
            return ScannerFailurePolicy.valueOf(value.trim().replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown scanner failure policy: " + value, exception);
        }
    }
}
