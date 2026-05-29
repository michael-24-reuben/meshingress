package dev.mrk.meshingress.artifact.security;

public interface ScannerAdapter {
    String name();

    ScannerResult scan(ScannerRequest request);
}
