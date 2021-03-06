package es.uvigo.det.labredes.dualeee;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.*;

/**
 * This class implements a discrete sequence of events sorted by event time.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class EventList {
    /**
     * The events list.
     */
    private List<Event> list;
    /**
     * The current instant of time.
     */
    private long simulation_time;
    /**
     * The end of time.
     */
    private long end_time;

    /**
     * Creates a new list of events.
     *
     * @param t the end of time
     */
    public EventList(long t) {
	list = new ArrayList<Event>();
	simulation_time = 0;
	end_time = t;
    }

    /**
     * Adds the specified event to the event list at the right position.
     *
     * @param event the Event to be added
     * @return true if the specified event is correctly added to the event list
     */
    public boolean addEvent (Event event) {
	if (event.time < simulation_time) {
	    DualModeEeeSimulator.printError("Trying to add an event with an invalid event time!");
	}
	if (event.time > end_time || list.contains(event)) {
	    return false;
	}	

	int i = 0;
	int list_size = list.size();
	while (i < list_size && event.compareTo(list.get(i)) <= 0) {
	    i++;
	}
	if (i == list_size) {
	    return list.add(event);
	}
	list.add(i, event);
	return true;
    }

    /**
     * Returns the time of the last event handled.
     *
     * @return the time of the last event handled
     */
    public long getSimulationTime () {
	return simulation_time;
    }

    /**
     * Returns the next event in the event list.
     *
     * @param remove if true the event is removed from the list
     * @return the next event in this event list or null if the list is empty
     */
    public Event getNextEvent (boolean remove) {
	Event event;
	try {
	    event = remove ? list.remove(0) : list.get(0);
	} catch (Exception e) {
	    event = null;
	}
	return event;
    }

    /**
     * Invokes the method that handles the specified event.
     *
     * @param event the Event to be handled
     */
    public void handleEvent (Event event) {
	simulation_time = event.time;
	try {
	    Method handler_method = DualModeEeeSimulator.link.getClass().getMethod(event.handler_method_name, event.getClass());
	    try {
		handler_method.invoke(DualModeEeeSimulator.link, event);
	    } catch (Exception e) {
		DualModeEeeSimulator.printError("Handler method invoke exception: " + event.handler_method_name + ": " + e.getMessage());
	    }
	} catch (Exception e) {
	    DualModeEeeSimulator.printError("Handler method exception: " + e.getMessage());
	}
    }

    /**
     * Prints on standard output a message for each event contained in the event list.
     */
    public void print () {
	for (int i = 0; i < list.size(); i++) {
	    list.get(i).print();
	}
    }

    /**
     * Removes the next state transition event from the event list. If the list does not contain the event, it is unchanged.
     *
     * @param state the state of the state transition event to be removed
     * @return true if a state transition event with the specified state is removed
     */
    public boolean removeStateTransitionEvent (EeeState state) {
	Event event;
	for (int i = 0; i < list.size(); i++) {
	    event = list.get(i);
	    if (event instanceof StateTransitionEvent && ((StateTransitionEvent) event).next_state == state) {
		list.remove(i);
		return true;
	    }
	}
	return false;
    }
}
