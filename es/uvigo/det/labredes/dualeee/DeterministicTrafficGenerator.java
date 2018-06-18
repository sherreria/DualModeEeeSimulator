package es.uvigo.det.labredes.dualeee;

/**
 * This class extends TrafficGenerator class to simulate deterministic arrivals.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class DeterministicTrafficGenerator extends TrafficGenerator {
    /**
     * Creates a new deterministic traffic generator.
     *
     * @param rate arrival rate (in b/s)
     * @param size frame size (in bytes)
     */
    public DeterministicTrafficGenerator (double rate, int size) {
	super(rate, size);
    }

    /**
     * Returns the instant at which the next frame arrives.
     *
     * @return instant at which the next frame arrives (in seconds)
     */
    public double getNextArrival () {
	arrival_time += 1 / frame_rate;
	return arrival_time;
    }
}
