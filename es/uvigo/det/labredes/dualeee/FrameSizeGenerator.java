package es.uvigo.det.labredes.dualeee;

import java.util.Random;

/**
 * This class generates random frame sizes.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
abstract public class FrameSizeGenerator {
    /**
     * The average frame size (in bytes).
     */
    public int frame_size;
    /**
     * The random number generator.
     */
    public Random rng;

    /**
     * Creates a new random frame size generator.
     *
     * @param fsize average frame size (in bytes)
     */
    public FrameSizeGenerator (int fsize) {
	frame_size = fsize;
	rng = new Random();
    }

    /**
     * Sets a new average frame size.
     *
     * @param fsize average frame size (in bytes)
     */
    public void setFrameSize (int fsize) {
	frame_size = fsize;
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
     * Returns the next frame size.
     *
     * @return the next frame size
     */
    abstract public int getNextFrameSize ();
}
