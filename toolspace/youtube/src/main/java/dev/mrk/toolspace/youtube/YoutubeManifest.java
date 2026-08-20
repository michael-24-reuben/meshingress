package dev.mrk.toolspace.youtube;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.*;

import java.util.List;

/**
 * Declares configuration names for the production property-file bridge and future OAuth adapters.
 */
public final class YoutubeManifest implements McpToolManifestDefinition {
    @Override
    public ToolModuleMetadata metadata() {
        return new ToolModuleMetadata(
                "youtube", "YouTube", "Permitted YouTube API operations", "Configuration for YouTube API and OAuth integrations.",
                List.of(), "", List.of("youtube", "media"), List.of(
                        ToolLink.documentation("https://developers.google.com/youtube/v3/guides/auth/server-side-web-apps").label("YouTube Data API OAuth guide"),
                        ToolLink.documentation("https://developers.google.com/youtube/analytics/reference/reports/query").label("YouTube Analytics reports query guide")
                ), new ToolIcon("youtube.svg", ToolIcon.MimeType.SVG_IMAGE, "YouTube"));
    }

    @Override
    public List<ToolProperty> properties() {
        return List.of(
                new ToolProperty(
                        "meshingress.youtube.api-key",
                        "Production property-file YouTube Data API key used only for permitted public reads.",
                        System.getenv("MESHINGRESS_YOUTUBE_API_KEY"),
                        "secret",
                        true,
                        true)
                        .required(false),
                ToolProperty.string("meshingress.youtube.oauth.client-id")
                        .description("Blank Google OAuth client ID for the Meshingress server application.")
                        .required(false),
                new ToolProperty("meshingress.youtube.oauth.client-secret", "Production property-file Google OAuth client secret reserved for the future OAuth flow.", "", "secret", true, true)
                        .required(false),
                ToolProperty.string("meshingress.youtube.oauth.redirect-uri")
                        .description("Blank registered HTTPS callback URI for the Google OAuth authorization-code flow.")
                        .required(false)
        );
    }

    @Override
    public List<ToolRequirement> requirements() {
        return List.of(ToolRequirement.scope(McpToolScope.NETWORK_OUTBOUND));
    }

    @Override
    public ToolReadme readme() {
        return ToolReadme.inline("""
                # YouTube credentials

                This manifest declares configuration names. The production property file is intentionally Git-ignored; leave every value blank until it is set on the production host.

                - `meshingress.youtube.api-key` is used by the implemented public YouTube Data API functions. Set it only in the ignored production property file.
                - `meshingress.youtube.oauth.client-id` is the Google Cloud OAuth client ID.
                - `meshingress.youtube.oauth.client-secret` is reserved for the future OAuth client.
                - `meshingress.youtube.oauth.redirect-uri` must exactly match the HTTPS redirect URI registered in Google Cloud.

                Per-account OAuth access and refresh tokens are intentionally not properties. They require an account ownership model, authorization-code callback, CSRF state validation, scope/grant checks, refresh and revocation behavior, and credential-safe audit events.
                """);
    }

    /*void main(String[] args) {
        System.out.println("YouTube manifest metadata:");
        System.out.println(metadata());
        System.out.println("YouTube manifest properties:");

        System.out.println("meshingress.youtube.api-key: " + System.getenv("MESHINGRESS_YOUTUBE_API_KEY"));
        System.out.println("meshingress.youtube.oauth.client-id: " + System.getenv("MESHINGRESS_YOUTUBE_OAUTH_CLIENT_ID"));
        System.out.println("meshingress.youtube.oauth.client-secret: " + System.getenv("MESHINGRESS_YOUTUBE_OAUTH_CLIENT_SECRET"));
        System.out.println("meshingress.youtube.oauth.redirect-uri: " + System.getenv("MESHINGRESS_YOUTUBE_OAUTH_REDIRECT_URI"));
        
        for (ToolProperty property : properties()) {
            System.out.println(property);
        }
        System.out.println("YouTube manifest requirements:");
        for (ToolRequirement requirement : requirements()) {
            System.out.println(requirement);
        }
    }*/
}
