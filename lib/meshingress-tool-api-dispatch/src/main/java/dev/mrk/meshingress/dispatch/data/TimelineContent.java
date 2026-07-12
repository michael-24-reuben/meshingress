package dev.mrk.meshingress.dispatch.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class TimelineContent extends StructuredContent {
    @Setter
    private String title;
    private List<TimelineEvent> events = new ArrayList<>();

    public TimelineContent() {
        super(StructuredContentKind.Data.DATA_TIMELINE);
    }

    public void setEvents(List<TimelineEvent> events) {
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }
}
