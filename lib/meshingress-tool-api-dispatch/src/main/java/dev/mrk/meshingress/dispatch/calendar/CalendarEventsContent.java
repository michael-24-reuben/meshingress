package dev.mrk.meshingress.dispatch.calendar;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class CalendarEventsContent extends StructuredContent {
    @Setter
    private String title;
    @Setter
    private String start;
    @Setter
    private String end;
    @Setter
    private String timezone;
    private List<CalendarEventContent> events = new ArrayList<>();

    public CalendarEventsContent() {
        super(StructuredContentKind.Time.CALENDAR_EVENTS);
    }

    public void setEvents(List<CalendarEventContent> events) {
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }
}
