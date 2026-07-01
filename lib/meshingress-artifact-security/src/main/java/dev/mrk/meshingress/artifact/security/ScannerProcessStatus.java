package dev.mrk.meshingress.artifact.security;

public enum ScannerProcessStatus {
    SUCCEEDED,
    NON_ZERO_EXIT,
    TIMED_OUT,
    START_FAILED,
    INTERRUPTED
}
