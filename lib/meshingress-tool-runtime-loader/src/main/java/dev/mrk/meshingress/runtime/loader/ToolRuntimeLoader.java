package dev.mrk.meshingress.runtime.loader;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactSource;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleHandle;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleStatus;

import java.util.List;
import java.util.Optional;

public interface ToolRuntimeLoader {
    ToolModuleHandle activate(ToolArtifactSource source);

    ToolModuleHandle activate(ResolvedToolArtifact artifact);

    void deactivate(ToolModuleId moduleId);

    Optional<ToolModuleStatus> status(ToolModuleId moduleId);

    List<ToolModuleStatus> list();
}
