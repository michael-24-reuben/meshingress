package dev.mrk.meshingress.mcp.tools;

/** Shared, non-secret result for listing and invocation eligibility. */
public record ToolAccessDecision(boolean visible, boolean executable, String reason) {
    public ToolAccessDecision { reason = reason == null ? "" : reason; }
    public static ToolAccessDecision allow() { return new ToolAccessDecision(true, true, ""); }
    public static ToolAccessDecision hidden(String reason) { return new ToolAccessDecision(false, false, reason); }
    public static ToolAccessDecision denied(String reason) { return new ToolAccessDecision(true, false, reason); }
}
