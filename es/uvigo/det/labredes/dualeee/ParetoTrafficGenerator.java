package es.uvigo.det.labredes.dualeee;

/**
 * This class extends TrafficGenerator class to simulate Pareto arrivals.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class ParetoTrafficGenerator extends TrafficGenerator {
    /**
     * The shape parameter.
     */
    private double alpha;
    /**
     * The scale parameter.
     */
    private double xm;

    /**
     * Creates a new Pareto traffic generator.
     *
     * @param rate arrival rate (in b/s)
     * @param size frame size (in bytes)
     * @param a shape parameter
     */
    public ParetoTrafficGenerator (double rate, int size, double a) {
	super(rate, size);
	alpha = a;
	xm = (alpha - 1) / alpha / frame_rate;
    }

    /**
     * Sets the shape parameter for the Pareto traffic generator.
     *
     * @param a shape parameter
     */
    public void setAlpha (double a) {
	alpha = a;
	xm = (alpha - 1) / alpha / frame_rate;
    }

    /**
     * Returns the instant at which the next frame arrives.
     *
     * @return instant at which the next frame arrives (in seconds)
     */
    public double getNextArrival () {
	arrival_time += xm / Math.pow(rng.nextDouble(), 1 / alpha);	
	return arrival_time;
    }    
}
