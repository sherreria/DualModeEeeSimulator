package es.uvigo.det.labredes.dualeee;

/**
 * This class extends TrafficGenerator class to simulate Poisson arrivals.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class PoissonTrafficGenerator extends TrafficGenerator {
    /**
     * Creates a new Poisson traffic generator.
     *
     * @param rate arrival rate (in b/s)
     * @param size frame size (in bytes)
     */
    public PoissonTrafficGenerator (double rate, int size) {
	super(rate, size);
    }

    /**
     * Returns the instant at which the next frame arrives.
     *
     * @return instant at which the next frame arrives (in seconds)
     */
    public double getNextArrival () {
	arrival_time -= Math.log(rng.nextDouble()) / frame_rate;
	return arrival_time;
    }    
}
