package es.uvigo.det.labredes.dualeee;

/**
 * This class extends Event class to simulate the arrival of a new frame.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class FrameArrivalEvent extends Event<EeeLink> {
    /**
     * The global frame counter.
     */
    static public long frame_counter = 0;
    /**
     * The unique identifier of the arriving frame.
     */
    public long frame_id;
    /**
     * The size of the arriving frame.
     */
    public int frame_size;

    /**
     * Creates a new event representing the arrival of a new frame.
     *
     * @param t      instant at which the new frame arrives
     * @param fsize  size of the new frame
     * @param method name of the method that handles the frame arrival
     */
    public FrameArrivalEvent (long t, int fsize, String method) {
	super(t, method);
	frame_id = frame_counter;
	frame_counter++;
	frame_size = fsize;
    }

    /**
     * Compares two frame arrival events.
     *
     * @param obj the Object to be compared
     * @return true if the specified event is equal to this event
     */
    public boolean equals (Object obj) {
	if (obj == this) {
	    return true;
	}
	if (!(obj instanceof FrameArrivalEvent)) {
            return false;
        }
	FrameArrivalEvent event = (FrameArrivalEvent) obj;
	if (time == event.time && frame_id == event.frame_id) {
	    return true;
	}
	return false;
    }

    /**
     * Prints on standard output a message describing the frame arrival event.
     */
    public void print () {
	System.out.format("%.3f FrameArrivalEvent %d %d %d %n", time / 1e6, frame_id, frame_size, DualModeEeeSimulator.link.queue_size);
    }
}
