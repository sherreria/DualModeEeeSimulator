package es.uvigo.det.labredes.dualeee;

import java.util.Random;

/**
 * This class simulates the arrival of a stream of frames of fixed size.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
abstract public class TrafficGenerator {
    /**
     * The frame rate (in frames/s).
     */
    public double frame_rate;
    /**
     * The frame size (in bits).
     */
    public int frame_size;
    /**
     * The instant at which the last frame arrived (in seconds).
     */
    public double arrival_time;
    /**
     * The random number generator.
     */
    public Random rng;

    /**
     * Creates a new random traffic generator.
     *
     * @param rate arrival rate (in b/s)
     * @param size frame size (in bytes)
     */
    public TrafficGenerator (double rate, int size) {
	frame_size = 8 * size;
	frame_rate = rate / frame_size;
	arrival_time = 0.0;
	rng = new Random();
    }

    /**
     * Sets a new arrival rate.
     *
     * @param rate arrival rate (in b/s)
     */
    public void setArrivalRate (double rate) {
	frame_rate = rate / frame_size;
    }

    /**
     * Sets a new frame size.
     *
     * @param size frame size (in bytes)
     */
    public void setFrameSize (int size) {
	double arrival_rate = Math.round(frame_rate * frame_size);
	frame_size = 8 * size;
	frame_rate = arrival_rate / frame_size;
    }

    /**
     * Sets the seed for the random number generator.
     *
     * @param seed initial seed
     */
    public void setSeed (long seed) {
	rng.setSeed(seed);
    }

    /**
     * Returns the instant at which the next frame arrives.
     *
     * @return instant at which the next frame arrives (in seconds)
     */
    abstract public double getNextArrival ();
}
