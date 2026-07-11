package dev.mrk.meshingress.dispatch.message;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class NotificationContent extends StructuredContent {
    @Setter
    private String title;
    @Setter
    private String body;
    @Setter
    private String severity;
    private List<NotificationAction> actions = new ArrayList<>();

    public NotificationContent() {
        super(StructuredContentKind.Message.MESSAGE_NOTIFICATION);
    }

    public void setActions(List<NotificationAction> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }
}
