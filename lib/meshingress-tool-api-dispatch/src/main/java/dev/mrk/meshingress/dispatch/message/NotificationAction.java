package dev.mrk.meshingress.dispatch.message;

public record NotificationAction(
        String label,
        String url,
        String action,
        String style
) { }
