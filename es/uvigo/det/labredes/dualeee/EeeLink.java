package es.uvigo.det.labredes.dualeee;

import java.util.Map;
import java.util.HashMap;

/**
 * This class simulates a dual-mode EEE link.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class EeeLink {
    /**
     * The link capacity.
     */
    public double capacity;
    /**
     * The traffic generator.
     */
    public TrafficGenerator traffic_generator;
    /**
     * The transmission queue.
     */
    public EventList queue;
    /**
     * The amount of frames stored in the transmission queue.
     */
    public int queue_size;
    /**
     * The maximum amount of frames that can be stored in the transmission queue.
     */
    public int max_queue_size;
    /**
     * The time required to transmit a frame on the link.
     */
    public long transmission_time;
    /**
     * The link state.
     */
    private EeeState state;

    // Statistics variables
    private long frames_received, frames_sent, frames_dropped;
    private long sum_frames_delay, maximum_frame_delay;
    private double prev_arrival_time, avg_arrival_rate;
    private long last_state_transition_time;
    private Map<EeeState, Long> time_in_states;
    private long num_coalescing_cycles;

    /**
     * Creates a new EEE link.
     * The traffic is simulated with the specified traffic generator.
     *
     * @param lc the link capacity (in b/s)
     * @param tg the traffic generator
     */
    public EeeLink (double lc, TrafficGenerator tg) {
	capacity = lc;
	traffic_generator = tg;
	transmission_time = (long) (1e12 * traffic_generator.frame_size / capacity);
        queue = new EventList(DualModeEeeSimulator.simulation_length);
        queue_size = max_queue_size = 0;

	last_state_transition_time = 0;
        time_in_states = new HashMap<EeeState, Long>();
        for (EeeState st : EeeState.values()) {
            time_in_states.put(st, (long) 0);
        }
	state = DualModeEeeSimulator.operation_mode.contains("deep") ? EeeState.TRANSITION_TO_DEEP : EeeState.TRANSITION_TO_FAST;
        DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (0, "handleStateTransitionEvent", state));

	frames_received = frames_sent = frames_dropped = 0;
	sum_frames_delay = maximum_frame_delay = 0;
	num_coalescing_cycles = 0;
	prev_arrival_time = avg_arrival_rate = 0.0;

	DualModeEeeSimulator.event_handler.addEvent(new FrameArrivalEvent ((long) (1e12 * traffic_generator.getNextArrival()), "handleFrameArrivalEvent"));
    }

    /**
     * Handles the specified frame arrival event.
     *
     * @param event the FrameArrivalEvent to be handled
     */
    public void handleFrameArrivalEvent (FrameArrivalEvent event) {
	frames_received++;
        if (max_queue_size == 0 || queue_size + 1 <= max_queue_size) {
            queue_size++;
            queue.addEvent(event);
            if (DualModeEeeSimulator.simulation_verbose) {
                event.print();
            }
        } else {
            DualModeEeeSimulator.event_handler.addEvent(new FrameDropEvent (event.time, "handleFrameDropEvent", event.frame_id));
        }
	if (DualModeEeeSimulator.operation_mode.contains("dyn")) {
	    double interarrival_time = event.time - prev_arrival_time;
	    if (avg_arrival_rate > 0) {
		avg_arrival_rate = 1.0 / interarrival_time + Math.exp(-0.5 * interarrival_time / DualModeEeeSimulator.max_delay) * (avg_arrival_rate - 1.0 / interarrival_time);
	    } else {
		avg_arrival_rate = 1.0 / interarrival_time;
	    }
	    prev_arrival_time = event.time;
	}
        DualModeEeeSimulator.event_handler.addEvent(new FrameArrivalEvent ((long) (1e12 * traffic_generator.getNextArrival()), "handleFrameArrivalEvent"));
	if (state == EeeState.FAST_WAKE && queue_size >= DualModeEeeSimulator.fast_to_active_qth) {
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_FAST));
	    if (DualModeEeeSimulator.operation_mode.equals("dual")) {
		DualModeEeeSimulator.event_handler.removeStateTransitionEvent(EeeState.TRANSITION_TO_DEEP);
	    }
	} else if (state == EeeState.DEEP_SLEEP && queue_size >= DualModeEeeSimulator.deep_to_active_qth) {
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP));
	}	
	if (DualModeEeeSimulator.max_delay > 0 && queue_size == 1 && state != EeeState.ACTIVE) {
	    long to_active_t = DualModeEeeSimulator.operation_mode.contains("fast") ? DualModeEeeSimulator.fast_to_active_t : DualModeEeeSimulator.deep_to_active_t;
	    long max_delay_event_time = event.time + DualModeEeeSimulator.max_delay - to_active_t;
	    EeeState transition_state = DualModeEeeSimulator.operation_mode.contains("fast") ? EeeState.TRANSITION_TO_ACTIVE_FROM_FAST : EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP;
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (max_delay_event_time, "handleStateTransitionEvent", transition_state));
	}
    }

    /**
     * Handles the specified frame drop event.
     *
     * @param event the FrameDropEvent to be handled
     */
    public void handleFrameDropEvent (FrameDropEvent event) {
        frames_dropped++;
        if (DualModeEeeSimulator.simulation_verbose) {
            event.print();
        }
    }

    /**
     * Handles the specified frame transmission event.
     *
     * @param event the FrameTransmissionEvent to be handled
     */
    public void handleFrameTransmissionEvent (FrameTransmissionEvent event) {
        if (queue_size == 0 || ((FrameArrivalEvent) (queue.getNextEvent(false))).frame_id != event.frame_id) {
	    event.print();
            DualModeEeeSimulator.printError("Trying to handle an invalid packet transmission!");
        }
	queue_size--;
        frames_sent++;
        long current_frame_delay = event.time - queue.getNextEvent(true).time - transmission_time;
        if (current_frame_delay > maximum_frame_delay) {
            maximum_frame_delay = current_frame_delay;
        }
        sum_frames_delay += current_frame_delay;
	if (DualModeEeeSimulator.simulation_verbose) {
            event.print();
        }
	if (queue_size > 0) {
	    long fid = ((FrameArrivalEvent) (queue.getNextEvent(false))).frame_id;
            DualModeEeeSimulator.event_handler.addEvent(new FrameTransmissionEvent (event.time + transmission_time, "handleFrameTransmissionEvent", fid));
	} else {
	    EeeState transition_state = DualModeEeeSimulator.operation_mode.contains("deep") ? EeeState.TRANSITION_TO_DEEP : EeeState.TRANSITION_TO_FAST;
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", transition_state));
	    if (DualModeEeeSimulator.operation_mode.equals("fast_dyn")) {
		DualModeEeeSimulator.fast_to_active_qth = (int) ((2 * DualModeEeeSimulator.target_delay - DualModeEeeSimulator.fast_to_active_t) * avg_arrival_rate + 1);
	    } else if (DualModeEeeSimulator.operation_mode.equals("deep_dyn")) {
		DualModeEeeSimulator.deep_to_active_qth = (int) ((2 * DualModeEeeSimulator.target_delay - DualModeEeeSimulator.deep_to_active_t) * avg_arrival_rate + 1);
	    }
	}
    }

    /**
     * Handles the specified state transition event.
     *
     * @param event the StateTransitionEvent to be handled
     */
    public void handleStateTransitionEvent (StateTransitionEvent event) {
	if (event.new_state == EeeState.ACTIVE) {
	    if (queue_size > 0) {
		long fid = ((FrameArrivalEvent) (queue.getNextEvent(false))).frame_id;
		DualModeEeeSimulator.event_handler.addEvent(new FrameTransmissionEvent (event.time + transmission_time, "handleFrameTransmissionEvent", fid));
		num_coalescing_cycles++;
	    }
	} else if (event.new_state == EeeState.FAST_WAKE) {
	    if (queue_size >= DualModeEeeSimulator.fast_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_FAST));
	    } else if (DualModeEeeSimulator.operation_mode.equals("dual")) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_fast_time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_DEEP));
	    }
	} else if (event.new_state == EeeState.DEEP_SLEEP) {
	    if (queue_size >= DualModeEeeSimulator.deep_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP));
	    }
	} else if (event.new_state == EeeState.TRANSITION_TO_FAST) {
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.active_to_fast_t, "handleStateTransitionEvent", EeeState.FAST_WAKE));
	} else if (event.new_state == EeeState.TRANSITION_TO_DEEP) {
	    long to_deep_t = DualModeEeeSimulator.operation_mode.equals("dual") ? DualModeEeeSimulator.fast_to_deep_t : DualModeEeeSimulator.active_to_fast_t + DualModeEeeSimulator.fast_to_deep_t;
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + to_deep_t, "handleStateTransitionEvent", EeeState.DEEP_SLEEP));
	} else if (event.new_state == EeeState.TRANSITION_TO_ACTIVE_FROM_FAST || event.new_state == EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP) {
	    if (DualModeEeeSimulator.max_delay > 0) {
		EeeState transition_state = DualModeEeeSimulator.operation_mode.contains("fast") ? EeeState.TRANSITION_TO_ACTIVE_FROM_FAST : EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP;
		DualModeEeeSimulator.event_handler.removeStateTransitionEvent(transition_state);
	    }
	    long to_active_t = event.new_state == EeeState.TRANSITION_TO_ACTIVE_FROM_FAST ? DualModeEeeSimulator.fast_to_active_t : DualModeEeeSimulator.deep_to_active_t;
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + to_active_t, "handleStateTransitionEvent", EeeState.ACTIVE));
	}

	time_in_states.put(state, time_in_states.get(state) + event.time - last_state_transition_time);
        state = event.new_state;
        last_state_transition_time = event.time;
        if (DualModeEeeSimulator.simulation_verbose) {
            event.print();
        }
    }

    /**
     * Prints on standard output some statistics.
     */
    public void printStatistics () {
        System.out.format("Frames: received %d sent %d dropped %d %n", frames_received, frames_sent, frames_dropped);
        if (frames_sent > 0) {
            System.out.format("Frame delay: average %.3f max %.3f %n", sum_frames_delay / 1e6 / frames_sent, maximum_frame_delay / 1e6);
        }
	time_in_states.put(state, time_in_states.get(state) + DualModeEeeSimulator.simulation_length - last_state_transition_time);
        for (EeeState st : EeeState.values()) {
            System.out.format("Time in state %s: %.3f %.2f %% %n", st, time_in_states.get(st) / 1e6, 100.0 * time_in_states.get(st) / DualModeEeeSimulator.simulation_length);
        }
	double power_consumption = (time_in_states.get(EeeState.ACTIVE) + 
				    time_in_states.get(EeeState.TRANSITION_TO_FAST) + time_in_states.get(EeeState.TRANSITION_TO_DEEP) +
				    time_in_states.get(EeeState.TRANSITION_TO_ACTIVE_FROM_FAST) + time_in_states.get(EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP) + 
				    DualModeEeeSimulator.fast_wake_consumption * time_in_states.get(EeeState.FAST_WAKE) +
				    DualModeEeeSimulator.deep_sleep_consumption * time_in_states.get(EeeState.DEEP_SLEEP)) / DualModeEeeSimulator.simulation_length;
	System.out.format("Power consumption: %.4f %n", power_consumption);
	System.out.format("Average coalescing cycle: %.4f %n", DualModeEeeSimulator.simulation_length / 1e6 / num_coalescing_cycles);
    }
}
