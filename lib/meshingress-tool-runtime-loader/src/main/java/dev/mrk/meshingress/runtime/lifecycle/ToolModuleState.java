package dev.mrk.meshingress.runtime.lifecycle;

public enum ToolModuleState {
    DISCOVERED,
    RESOLVING,
    RESOLVED,
    LOADING,
    LOADED,
    STARTING,
    ACTIVE,
    QUIESCING,
    STOPPING,
    STOPPED,
    UNLOADED,
    FAILED
}
