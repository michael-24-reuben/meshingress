package dev.mrk.meshingress.dispatch.calendar;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class CalendarEventContent extends StructuredContent {
    @Setter
    private String id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String start;
    @Setter
    private String end;
    @Setter
    private String timezone;
    @Setter
    private String location;
    private List<String> attendees = new ArrayList<>();

    public CalendarEventContent() {
        super(StructuredContentKind.Time.CALENDAR_EVENT);
    }

    public void setAttendees(List<String> attendees) {
        this.attendees = attendees == null ? new ArrayList<>() : new ArrayList<>(attendees);
    }
}
