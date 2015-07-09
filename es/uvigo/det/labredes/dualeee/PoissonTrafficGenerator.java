package es.uvigo.det.labredes.dualeee;

/**
 * This class extends TrafficGenerator class to simulate Poisson traffic.
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
	double rand = rng.nextDouble();
	arrival_time += -1.0 * Math.log(rand) / frame_rate;
	return arrival_time;
    }    
}
