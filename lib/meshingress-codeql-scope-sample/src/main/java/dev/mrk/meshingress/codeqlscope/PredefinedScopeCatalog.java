package dev.mrk.meshingress.codeqlscope;

import java.util.List;

public final class PredefinedScopeCatalog {
    private PredefinedScopeCatalog() {
    }

    public static List<ScopeRule> sampleRules() {
        return List.of(
                new ScopeRule("java-files-read", "FILES_READ", List.of(
                        ScopeMatcher.bytecodeMethod("java/nio/file/Files", "read.*", "high"),
                        ScopeMatcher.bytecodeMethod("java/io/FileInputStream", ".*", "high"),
                        ScopeMatcher.codeQlCall("java.nio.file.Files", "read%", "high"),
                        ScopeMatcher.regex("new\\s+File\\s*\\(", "low", true)
                )),
                new ScopeRule("java-files-write", "FILES_WRITE", List.of(
                        ScopeMatcher.bytecodeMethod("java/nio/file/Files", "write.*", "high"),
                        ScopeMatcher.bytecodeMethod("java/io/FileOutputStream", ".*", "high"),
                        ScopeMatcher.codeQlCall("java.nio.file.Files", "write%", "high")
                )),
                new ScopeRule("java-files-delete", "FILES_DELETE", List.of(
                        ScopeMatcher.bytecodeMethod("java/nio/file/Files", "delete.*", "high"),
                        ScopeMatcher.codeQlCall("java.nio.file.Files", "delete%", "high")
                )),
                new ScopeRule("java-process-execute", "SHELL_EXECUTE", List.of(
                        ScopeMatcher.bytecodeMethod("java/lang/ProcessBuilder", ".*", "high"),
                        ScopeMatcher.bytecodeMethod("java/lang/Runtime", "exec", "high"),
                        ScopeMatcher.codeQlCall("java.lang.Runtime", "exec", "high")
                )),
                new ScopeRule("java-network-outbound", "NETWORK_ACCESS", List.of(
                        ScopeMatcher.bytecodeMethod("java/net/http/HttpClient", ".*", "high"),
                        ScopeMatcher.bytecodeMethod("java/net/Socket", ".*", "high"),
                        ScopeMatcher.bytecodeMethod("java/net/URL", "openConnection", "medium"),
                        ScopeMatcher.codeQlCall("java.net.URL", "openConnection", "medium")
                )),
                new ScopeRule("meshingress-cache-result", "CACHE_WRITE", List.of(
                        ScopeMatcher.bytecodeAnnotation("dev/mrk/meshingress/api/tools/annotation/McpCacheResult", "medium")
                ))
        );
    }
}
