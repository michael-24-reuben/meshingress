package dev.mrk.meshingress.dispatch.identity;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class UserContent extends StructuredContent {
    private String id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;

    public UserContent() {
        super(StructuredContentKind.Identity.IDENTITY_USER);
    }

}
