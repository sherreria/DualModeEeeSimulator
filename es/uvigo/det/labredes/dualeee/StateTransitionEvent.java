package es.uvigo.det.labredes.dualeee;

/**
 * This class extends Event class to simulate state transitions at the link.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class StateTransitionEvent extends Event<EeeLink> {
    /**
     * The new state of the link.
     */
    public EeeState new_state;

    /**
     * Creates a new event representing a state transition at the link.
     *
     * @param t      instant at which the link changes its state
     * @param method name of the method that handles the state transition
     * @param state  new state of the link
     */
    public StateTransitionEvent (long t, String method, EeeState state) {
	super(t, method);
	new_state = state;
    }

    /**
     * Compares two state transition events.
     *
     * @param obj the Object to be compared
     * @return true if the specified event is equal to this event
     */
    public boolean equals (Object obj) {
	if (obj == this) {
	    return true;
	}
	if (!(obj instanceof StateTransitionEvent)) {
            return false;
        }
	StateTransitionEvent event = (StateTransitionEvent) obj;
	if (time == event.time && new_state == event.new_state) {
	    return true;
	}
	return false;
    }

    /**
     * Prints on standard output a message describing the state transition event.
     */
    public void print () {
	System.out.format("%.3f StateTransitionEvent %s %n", time / 1e6, new_state);
    }
}
