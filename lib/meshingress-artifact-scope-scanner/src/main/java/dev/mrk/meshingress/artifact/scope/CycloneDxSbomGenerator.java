package dev.mrk.meshingress.artifact.scope;

import java.io.IOException;
import java.nio.file.Path;

public interface CycloneDxSbomGenerator {
    CycloneDxSbom generate(Path artifact) throws IOException;
}
