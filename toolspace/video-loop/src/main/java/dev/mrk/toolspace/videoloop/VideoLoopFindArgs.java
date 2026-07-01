package dev.mrk.toolspace.videoloop;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record VideoLoopFindArgs(
        @McpInputField(value = "videoPath", description = "Absolute or workspace-relative input video path.")
        String videoPath,

        @McpInputField(value = "startFrame", description = "Frame index chosen as the loop beginning.")
        int startFrame,

        @McpInputField(value = "durationHint", description = "Expected loop duration in frames. Used to center the search window.", required = false)
        Integer durationHint,

        @McpInputField(value = "searchRadius", description = "Number of frames around durationHint to search.", required = false)
        Integer searchRadius,

        @McpInputField(value = "outputPath", description = "Optional output clip path. When omitted, only candidates are returned.", required = false)
        String outputPath,

        @McpInputField(value = "python", description = "Python executable. Defaults to python.", required = false)
        String python,

        @McpInputField(value = "vendorScript", description = "Optional path to video_loop_finder.py. Defaults to Meshingress/vendor/media/video-loop-finder/video_loop_finder.py.", required = false)
        String vendorScript
) {
    public String pythonOrDefault() {
        return python == null || python.isBlank() ? "python" : python;
    }

    public int searchRadiusOrDefault() {
        return searchRadius == null || searchRadius <= 0 ? 80 : searchRadius;
    }
}
