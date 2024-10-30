package tr.com.logidex.cnetdedicated.fxcontrols;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * The TagWroteEvent class represents an event that is fired when a tag is written.
 * It extends the Event class.
 */
public class TagWroteEvent extends Event {
    public static final EventType<TagWroteEvent> et = new EventType<>(Event.ANY, "Button tag write event");
    private String data;


    public TagWroteEvent() {
        super(et);
    }


    public String getData() {
        return data;
    }


    public void setData(String s) {
        data = s;
    }
}
