package dev.mrk.meshingress.dispatch.data;

public record TableColumn(
        String key,
        String label,
        String type,
        boolean sortable
) { }
