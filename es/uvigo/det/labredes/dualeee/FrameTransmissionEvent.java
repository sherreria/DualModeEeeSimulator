package es.uvigo.det.labredes.dualeee;

/**
 * This class extends Event class to simulate the transmission of a frame.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class FrameTransmissionEvent extends Event<EeeLink> {
    /**
     * The unique identifier of the frame transmitted.
     */
    public long frame_id;

    /**
     * Creates a new event representing the transmission of a frame.
     *
     * @param t      instant at which the link ends the transmission
     * @param method name of the method that handles the transmission
     * @param fid    identifier of the frame transmitted
     */
    public FrameTransmissionEvent (long t, String method, long fid) {
	super(t, method);
	frame_id = fid;
    }

    /**
     * Compares two packet transmission events.
     *
     * @param obj the Object to be compared
     * @return true if the specified event is equal to this event
     */
    public boolean equals (Object obj) {
	if (obj == this) {
	    return true;
	}
	if (!(obj instanceof FrameTransmissionEvent)) {
            return false;
        }
	FrameTransmissionEvent event = (FrameTransmissionEvent) obj;
	if (time == event.time && frame_id == event.frame_id) {
	    return true;
	}
	return false;
    }

    /**
     * Prints on standard output a message describing the frame transmission event.
     */
    public void print () {
	System.out.format("%.3f FrameTransmissionEvent %d %d %n", time / 1e6, frame_id, DualModeEeeSimulator.link.queue_size);
    }
}
