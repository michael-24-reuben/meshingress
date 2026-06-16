package dev.mrk.meshingress.artifact.security;

import java.util.List;
import java.util.Map;

public class FakeScanner implements ScannerAdapter {

    @Override
    public String name() {
        return "fake-scanner";
    }

    @Override
    public ScannerResult scan(ScannerRequest request) {
        return new ScannerResult(
                name(),
                "0.0.0-dev",
                ScannerStatus.PASSED,
                List.of(),
                Map.of(
                        "mode", "fake",
                        "artifact", request.coordinate().display(),
                        "fileCount", request.fileEntries().size()
                ),
                null
        );
    }
}
