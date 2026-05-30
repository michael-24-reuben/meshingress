package dev.mrk.toolspace.powershellcli;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

import java.util.List;
import java.util.Map;

public record PowerShellExecuteArgs(
        @McpInputField(
                value = "script",
                description = "PowerShell script body to execute.",
                required = true
        )
        String script,

        @McpInputField(
                value = "workingDirectory",
                description = "Optional working directory. Defaults to the current JVM working directory.",
                required = false
        )
        String workingDirectory,

        @McpInputField(
                value = "timeoutMs",
                description = "Optional execution timeout in milliseconds. Defaults to 20000 and is capped by the tool.",
                required = false
        )
        Long timeoutMs,

        @McpInputField(
                value = "executable",
                description = "Optional PowerShell executable. Defaults to pwsh. Use powershell.exe for Windows PowerShell.",
                required = false
        )
        String executable,

        @McpInputField(
                value = "arguments",
                description = "Optional script arguments passed after -File.",
                required = false
        )
        List<String> arguments,

        @McpInputField(
                value = "environment",
                description = "Optional environment variables to add or override for the child process.",
                required = false
        )
        Map<String, String> environment,

        @McpInputField(
                value = "includeScriptInStructuredContent",
                description = "Whether to echo the script body in structuredContent. Defaults to false.",
                required = false
        )
        Boolean includeScriptInStructuredContent
) {
    public long normalizedTimeoutMs() {
        long value = timeoutMs == null ? 20_000L : timeoutMs;
        if (value <= 0) {
            return 20_000L;
        }
        return Math.min(value, 120_000L);
    }

    public String normalizedExecutable() {
        if (executable == null || executable.isBlank()) {
            return "pwsh";
        }
        return executable.trim();
    }

    public boolean shouldIncludeScriptInStructuredContent() {
        return Boolean.TRUE.equals(includeScriptInStructuredContent);
    }
}
