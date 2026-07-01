package dev.mrk.toolspace.cobalt;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record CobaltProcessArgs(
        @McpInputField(value = "url", description = "Public media URL to process through the configured Cobalt API instance.")
        String url,

        @McpInputField(value = "audioBitrate", description = "Optional audio bitrate: 320, 256, 128, 96, 64, or 8.", required = false)
        String audioBitrate,

        @McpInputField(value = "audioFormat", description = "Optional audio format: best, mp3, ogg, wav, or opus.", required = false)
        String audioFormat,

        @McpInputField(value = "downloadMode", description = "Optional download mode: auto, audio, or mute.", required = false)
        String downloadMode,

        @McpInputField(value = "filenameStyle", description = "Optional filename style: classic, pretty, basic, or nerdy.", required = false)
        String filenameStyle,

        @McpInputField(value = "videoQuality", description = "Optional video quality: max, 4320, 2160, 1440, 1080, 720, 480, 360, 240, or 144.", required = false)
        String videoQuality,

        @McpInputField(value = "disableMetadata", description = "When true, request no embedded title, artist, or other metadata.", required = false)
        Boolean disableMetadata,

        @McpInputField(value = "alwaysProxy", description = "When true, request Cobalt to tunnel all files even when direct redirects are available.", required = false)
        Boolean alwaysProxy,

        @McpInputField(value = "localProcessing", description = "Optional local processing preference: disabled, preferred, or forced.", required = false)
        String localProcessing,

        @McpInputField(value = "subtitleLang", description = "Optional subtitle language code.", required = false)
        String subtitleLang,

        @McpInputField(value = "youtubeVideoCodec", description = "Optional YouTube video codec: h264, av1, or vp9.", required = false)
        String youtubeVideoCodec,

        @McpInputField(value = "youtubeVideoContainer", description = "Optional YouTube video container: auto, mp4, webm, or mkv.", required = false)
        String youtubeVideoContainer,

        @McpInputField(value = "youtubeDubLang", description = "Optional YouTube dub language code.", required = false)
        String youtubeDubLang,

        @McpInputField(value = "convertGif", description = "When true, request Twitter/X GIF conversion to GIF format.", required = false)
        Boolean convertGif,

        @McpInputField(value = "allowH265", description = "When true, allow H265/HEVC videos from TikTok.", required = false)
        Boolean allowH265,

        @McpInputField(value = "tiktokFullAudio", description = "When true, request original full audio for TikTok videos.", required = false)
        Boolean tiktokFullAudio,

        @McpInputField(value = "youtubeBetterAudio", description = "When true, prefer higher-quality YouTube audio when available.", required = false)
        Boolean youtubeBetterAudio,

        @McpInputField(value = "youtubeHLS", description = "When true, allow YouTube HLS formats if the configured Cobalt instance supports them.", required = false)
        Boolean youtubeHLS
) {
}
