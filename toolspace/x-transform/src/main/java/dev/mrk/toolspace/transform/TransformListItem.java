package dev.mrk.toolspace.transform;

public record TransformListItem(String type, String id, String outputLanguage) {
    static TransformListItem from(TransformType type) { return new TransformListItem(type.name(), type.id(), type.outputLanguage()); }
}
