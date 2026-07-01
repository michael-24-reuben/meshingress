package dev.mrk.meshingress.dispatch.identity;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class AccountContent extends StructuredContent {
    private String id;
    private String provider;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String url;

    public AccountContent() {
        super(StructuredContentKind.Identity.IDENTITY_ACCOUNT);
    }

}
