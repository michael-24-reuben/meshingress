package dev.mrk.toolspace.webtoon;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;

public record WebtoonInspectArgs(
        @McpInputField(
                value = "url",
                description = "Public WEBTOON series URL, for example https://www.webtoons.com/en/.../list?title_no=123.",
                required = true
        )
        String url
) {
}
