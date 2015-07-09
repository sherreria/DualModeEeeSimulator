package es.uvigo.det.labredes.dualeee;

/**
 * This class extends Event class to simulate the drop of a new arriving frame.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class FrameDropEvent extends Event<EeeLink> {
    /**
     * The unique identifier of the discarded frame.
     */
    public long frame_id;

    /**
     * Creates a new event representing the drop of a new arriving frame.
     *
     * @param t      instant at which the new arriving frame is discarded
     * @param method name of the method that handles the frame drop
     * @param fid    identifier of the discarded frame
     */
    public FrameDropEvent (long t, String method, long fid) {
	super(t, method);
	frame_id = fid;
    }

    /**
     * Compares two frame drop events.
     *
     * @param obj the Object to be compared
     * @return true if the specified event is equal to this event
     */
    public boolean equals (Object obj) {
	if (obj == this) {
	    return true;
	}
	if (!(obj instanceof FrameDropEvent)) {
            return false;
        }
	FrameDropEvent event = (FrameDropEvent) obj;
	if (time == event.time && frame_id == event.frame_id) {
	    return true;
	}
	return false;
    }

    /**
     * Prints on standard output a message describing the frame drop event.
     */
    public void print () {
	System.out.format("%.3f FrameDropEvent %d %d %n", time / 1e6, frame_id, DualModeEeeSimulator.link.queue_size);
    }
}
