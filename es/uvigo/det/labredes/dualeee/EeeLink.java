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
    public long capacity;
    /**
     * The traffic generator.
     */
    public TrafficGenerator traffic_generator;
    /**
     * The frame size generator.
     */
    public FrameSizeGenerator frame_size_generator;   
    /**
     * The transmission buffer.
     */
    public EventList queue;
    /**
     * The current amount of frames queued in the transmission buffer.
     */
    public int queue_size;
    /**
     * The maximum amount of frames that can be queued in the transmission buffer.
     */
    public int max_queue_size;
    /**
     * The current link state.
     */
    private EeeState state;

    // Statistics variables
    private long frames_received, frames_sent, frames_dropped, bytes_received;
    private long sum_frames_delay, maximum_frame_delay;
    private long last_state_transition_time;
    private Map<EeeState, Long> time_in_states;
    private long num_coalescing_cycles;

    // Mostowfi coalescing variables
    private int mostowfi_queue_size;

    // Dynamic coalescing variables
    private EeeState prev_transition_state;
    private double delay_th, arrival_rate_th;
    private double weighted_sum_active_qth, weighted_sum_active_max_delay, prev_update_active;
    private int frames_received_in_current_cycle, bytes_received_in_current_cycle;

    /**
     * Creates a new EEE link.
     * Data traffic is simulated with the specified traffic and frame size generators.
     *
     * @param lc  the link capacity (in b/s)
     * @param tg  the traffic generator
     * @param fsg the frame size generator
     */
    public EeeLink (long lc, TrafficGenerator tg, FrameSizeGenerator fsg) {
	capacity = lc;
	traffic_generator = tg;
	frame_size_generator = fsg;
        queue = new EventList(DualModeEeeSimulator.simulation_length);
        queue_size = max_queue_size = 0;

	last_state_transition_time = 0;
        time_in_states = new HashMap<EeeState, Long>();
        for (EeeState st : EeeState.values()) {
            time_in_states.put(st, (long) 0);
        }

	if (DualModeEeeSimulator.operation_mode.equals("dual_dyn")) {
	    double c = (1 - DualModeEeeSimulator.deep_sleep_consumption) / (1 - DualModeEeeSimulator.fast_wake_consumption);
	    double a = c * (DualModeEeeSimulator.active_to_fast_t + DualModeEeeSimulator.fast_to_deep_t) * DualModeEeeSimulator.fast_to_active_t;
	    a -= DualModeEeeSimulator.active_to_fast_t * DualModeEeeSimulator.deep_to_active_t;
	    double b = DualModeEeeSimulator.deep_to_active_t - DualModeEeeSimulator.active_to_fast_t;
	    b += c * (DualModeEeeSimulator.active_to_fast_t + DualModeEeeSimulator.fast_to_deep_t - DualModeEeeSimulator.fast_to_active_t);
	    delay_th = DualModeEeeSimulator.deep_to_active_t / 2.0 + a / (Math.sqrt(b*b-4*a*(1-c)) - b);
	    arrival_rate_th = 1 / (DualModeEeeSimulator.deep_to_active_t - 2 * DualModeEeeSimulator.target_delay + 2 * a / (Math.sqrt(b*b-4*a*(1-c)) - b));
	}
	
	state = DualModeEeeSimulator.operation_mode.contains("deep") || (DualModeEeeSimulator.operation_mode.equals("dual_dyn") && DualModeEeeSimulator.target_delay > delay_th) ? 
	    EeeState.TRANSITION_TO_DEEP : EeeState.TRANSITION_TO_FAST;
	prev_transition_state = state;
        DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (0, "handleStateTransitionEvent", state));

	frames_received = frames_sent = frames_dropped = bytes_received = 0;
	sum_frames_delay = maximum_frame_delay = 0;
	num_coalescing_cycles = 0;
	weighted_sum_active_qth = weighted_sum_active_max_delay = 0.0;
	prev_update_active = 0.0;  
	frames_received_in_current_cycle = bytes_received_in_current_cycle = 0;
	
	DualModeEeeSimulator.event_handler.addEvent(new FrameArrivalEvent ((long) (1e12 * traffic_generator.getNextArrival()), frame_size_generator.getNextFrameSize(), "handleFrameArrivalEvent"));
    }

    /**
     * Handles the specified frame arrival event.
     *
     * @param event the FrameArrivalEvent to be handled
     */
    public void handleFrameArrivalEvent (FrameArrivalEvent event) {
	frames_received++;
	bytes_received += event.frame_size;
	DualModeEeeSimulator.event_handler.addEvent(new FrameArrivalEvent ((long) (1e12 * traffic_generator.getNextArrival()), frame_size_generator.getNextFrameSize(), "handleFrameArrivalEvent"));
	
        if (max_queue_size == 0 || queue_size < max_queue_size) {
            queue_size++;
            queue.addEvent(event);
            if (DualModeEeeSimulator.simulation_verbose) {
                event.print();
            }
        } else {
            DualModeEeeSimulator.event_handler.addEvent(new FrameDropEvent (event.time, "handleFrameDropEvent", event.frame_id));
	    return;
        }
	
	if (!DualModeEeeSimulator.operation_mode.contains("mul")) {
	    if (DualModeEeeSimulator.operation_mode.contains("dyn")) {
		frames_received_in_current_cycle++;
		bytes_received_in_current_cycle += event.frame_size;
	    }
	    if (state == EeeState.FAST_WAKE && DualModeEeeSimulator.fast_to_active_qth > 0 && queue_size >= DualModeEeeSimulator.fast_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_FAST));
		if (DualModeEeeSimulator.operation_mode.equals("dual")) {
		    DualModeEeeSimulator.event_handler.removeStateTransitionEvent(EeeState.TRANSITION_TO_DEEP);
		}
	    } else if (state == EeeState.DEEP_SLEEP && DualModeEeeSimulator.deep_to_active_qth > 0 && queue_size >= DualModeEeeSimulator.deep_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP));
	    } else if (DualModeEeeSimulator.max_delay > 0 && queue_size == 1 && state != EeeState.ACTIVE) {
		if (!DualModeEeeSimulator.operation_mode.equals("mostowfi") || state == EeeState.TRANSITION_TO_DEEP || state == EeeState.DEEP_SLEEP) {
		    EeeState transition_state = DualModeEeeSimulator.operation_mode.contains("fast") ||
			(DualModeEeeSimulator.operation_mode.equals("dual_dyn") && prev_transition_state == EeeState.TRANSITION_TO_FAST) ?
			EeeState.TRANSITION_TO_ACTIVE_FROM_FAST : EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP;
		    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_delay, "handleStateTransitionEvent", transition_state));
		}
	    }
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
        event.frame_delay = event.time - queue.getNextEvent(true).time - event.frame_time;
        if (event.frame_delay > maximum_frame_delay) {
            maximum_frame_delay = event.frame_delay;
        }
        sum_frames_delay += event.frame_delay;
	if (DualModeEeeSimulator.simulation_verbose) {
            event.print();
        }
	if (queue_size > 0) {
	    FrameArrivalEvent next_frame = (FrameArrivalEvent) (queue.getNextEvent(false));
	    long next_frame_time = (long) (8e12 * next_frame.frame_size / capacity);
            DualModeEeeSimulator.event_handler.addEvent(new FrameTransmissionEvent (event.time + next_frame_time, "handleFrameTransmissionEvent", next_frame.frame_id, next_frame_time));
	} else {
	    EeeState transition_state = EeeState.TRANSITION_TO_DEEP;
	    if (DualModeEeeSimulator.operation_mode.contains("fast") ||
		(DualModeEeeSimulator.operation_mode.equals("mostowfi") && mostowfi_queue_size >= DualModeEeeSimulator.deep_to_active_qth / 2.0)) {
		transition_state = EeeState.TRANSITION_TO_FAST;
	    }
	    if (DualModeEeeSimulator.operation_mode.contains("dyn")) {
		double avg_arrival_rate = frames_received_in_current_cycle / (event.time - prev_update_active);
		double utilization_factor = bytes_received_in_current_cycle * 8e12 / (event.time - prev_update_active) / capacity;
		long w0 = (long) ((1 + Math.pow(1 - utilization_factor, 2)) / (2 * avg_arrival_rate * (1 - utilization_factor)));
		if (DualModeEeeSimulator.operation_mode.contains("time")) {
		     weighted_sum_active_max_delay += DualModeEeeSimulator.max_delay * (event.time - prev_update_active);
		     DualModeEeeSimulator.max_delay = DualModeEeeSimulator.target_delay - w0;
		     DualModeEeeSimulator.max_delay += (long) (Math.sqrt(1 + Math.pow(1 + avg_arrival_rate * (DualModeEeeSimulator.target_delay - w0), 2)) / avg_arrival_rate);
		     DualModeEeeSimulator.max_delay -= transition_state == EeeState.TRANSITION_TO_FAST ? DualModeEeeSimulator.fast_to_active_t : DualModeEeeSimulator.deep_to_active_t;
		     if (DualModeEeeSimulator.max_delay <= 1e3) { // 1ns
			 DualModeEeeSimulator.max_delay = (long) 1e3;
		     }
		} else {
		    int active_qth = prev_transition_state == EeeState.TRANSITION_TO_FAST ? DualModeEeeSimulator.fast_to_active_qth : DualModeEeeSimulator.deep_to_active_qth;
		    weighted_sum_active_qth += active_qth * (event.time - prev_update_active);
		    if (transition_state == EeeState.TRANSITION_TO_FAST) {
			DualModeEeeSimulator.fast_to_active_qth = (int) Math.floor(2 * avg_arrival_rate * (DualModeEeeSimulator.target_delay - w0 - DualModeEeeSimulator.fast_to_active_t/2) + 3);
			if (DualModeEeeSimulator.fast_to_active_qth < 1) {
			    DualModeEeeSimulator.fast_to_active_qth = 1;
			}
		    } else {
			DualModeEeeSimulator.deep_to_active_qth = (int) Math.floor(2 * avg_arrival_rate * (DualModeEeeSimulator.target_delay - w0 - DualModeEeeSimulator.deep_to_active_t/2) + 3);
			if (DualModeEeeSimulator.deep_to_active_qth < 1) {
			    DualModeEeeSimulator.deep_to_active_qth = 1;
			}
		    }
		}
		if ((DualModeEeeSimulator.operation_mode.equals("dual_dyn") && DualModeEeeSimulator.target_delay < DualModeEeeSimulator.deep_to_active_t / 2.0) ||
		    (DualModeEeeSimulator.operation_mode.equals("dual_dyn") && DualModeEeeSimulator.target_delay < delay_th && avg_arrival_rate > arrival_rate_th)) {
		    transition_state = EeeState.TRANSITION_TO_FAST;
		}
		prev_transition_state = transition_state;
		prev_update_active = event.time;
		frames_received_in_current_cycle = bytes_received_in_current_cycle = 0;
	    }
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", transition_state));
	}
    }

    /**
     * Handles the specified state transition event.
     *
     * @param event the StateTransitionEvent to be handled
     */
    public void handleStateTransitionEvent (StateTransitionEvent event) {
	if (event.next_state == EeeState.ACTIVE) {
	    if (queue_size > 0) {
		num_coalescing_cycles++;
		FrameArrivalEvent next_frame = (FrameArrivalEvent) (queue.getNextEvent(false));
		long next_frame_time = (long) (8e12 * next_frame.frame_size / capacity);
		DualModeEeeSimulator.event_handler.addEvent(new FrameTransmissionEvent (event.time + next_frame_time, "handleFrameTransmissionEvent", next_frame.frame_id, next_frame_time));
	    } else {
		DualModeEeeSimulator.printError("Trying to activate the link with no packet to transmit!");
	    }
	} else if (event.next_state == EeeState.FAST_WAKE) {
	    if (DualModeEeeSimulator.fast_to_active_qth > 0 && queue_size >= DualModeEeeSimulator.fast_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_FAST));
	    } else if (DualModeEeeSimulator.operation_mode.equals("dual")) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_fast_wake_time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_DEEP));
	    } else if (DualModeEeeSimulator.operation_mode.equals("mostowfi")) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_fast_wake_time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_FAST));
	    } else if (DualModeEeeSimulator.operation_mode.contains("mul")) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_delay, "handleStateTransitionEvent", EeeState.FAST_WAKE));
	    }
	} else if (event.next_state == EeeState.DEEP_SLEEP) {
	    if (DualModeEeeSimulator.deep_to_active_qth > 0 && queue_size >= DualModeEeeSimulator.deep_to_active_qth) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP));
	    } else if (DualModeEeeSimulator.operation_mode.contains("mul")) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.max_delay, "handleStateTransitionEvent", EeeState.DEEP_SLEEP));
	    }
	} else if (event.next_state == EeeState.TRANSITION_TO_FAST) {
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + DualModeEeeSimulator.active_to_fast_t, "handleStateTransitionEvent", EeeState.FAST_WAKE));
	} else if (event.next_state == EeeState.TRANSITION_TO_DEEP) {
	    long to_deep_t = DualModeEeeSimulator.operation_mode.equals("dual") ? DualModeEeeSimulator.fast_to_deep_t : DualModeEeeSimulator.active_to_fast_t + DualModeEeeSimulator.fast_to_deep_t;
	    DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + to_deep_t, "handleStateTransitionEvent", EeeState.DEEP_SLEEP));
	} else if (event.next_state == EeeState.TRANSITION_TO_ACTIVE_FROM_FAST || event.next_state == EeeState.TRANSITION_TO_ACTIVE_FROM_DEEP) {
	    if (DualModeEeeSimulator.max_delay > 0) {
		DualModeEeeSimulator.event_handler.removeStateTransitionEvent(event.next_state);
	    }
	    if (DualModeEeeSimulator.operation_mode.equals("mostowfi") && queue_size == 0) {
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time, "handleStateTransitionEvent", EeeState.TRANSITION_TO_DEEP));
	    } else {
		mostowfi_queue_size = queue_size;
		long to_active_t = event.next_state == EeeState.TRANSITION_TO_ACTIVE_FROM_FAST ? DualModeEeeSimulator.fast_to_active_t : DualModeEeeSimulator.deep_to_active_t;
		DualModeEeeSimulator.event_handler.addEvent(new StateTransitionEvent (event.time + to_active_t, "handleStateTransitionEvent", EeeState.ACTIVE));
	    }
	}

	time_in_states.put(state, time_in_states.get(state) + event.time - last_state_transition_time);
        state = event.next_state;
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
	    System.out.format("Average frame size: %.3f %n", 1.0 * bytes_received / frames_received);
	    System.out.format("Average bit rate: %.3f %n", 8e12 * bytes_received / DualModeEeeSimulator.simulation_length);
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
	System.out.format("Power consumption: %.3f %n", power_consumption);
	System.out.format("Average coalescing cycle: %.3f %n", DualModeEeeSimulator.simulation_length / 1e6 / num_coalescing_cycles);
	if (DualModeEeeSimulator.operation_mode.contains("dyn")) {
	    if (DualModeEeeSimulator.operation_mode.contains("time")) {
		weighted_sum_active_max_delay += DualModeEeeSimulator.max_delay * (DualModeEeeSimulator.simulation_length - prev_update_active);
		System.out.format("Average coalescing max delay: %.3f %n", weighted_sum_active_max_delay / 1e6 / DualModeEeeSimulator.simulation_length);
	    } else {
		int active_qth = prev_transition_state == EeeState.TRANSITION_TO_FAST ? DualModeEeeSimulator.fast_to_active_qth : DualModeEeeSimulator.deep_to_active_qth;
		weighted_sum_active_qth += active_qth * (DualModeEeeSimulator.simulation_length - prev_update_active);
		System.out.format("Average coalescing queue threshold: %.3f %n", weighted_sum_active_qth / DualModeEeeSimulator.simulation_length);
	    }
	}
    }
}
