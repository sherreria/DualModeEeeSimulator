package es.uvigo.det.labredes.dualeee;

/**
 * This class extends FrameSizeGenerator class assuming that frame sizes
 * follow a uniform distribution.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class UniformFrameSizeGenerator extends FrameSizeGenerator {
    /**
     * The minimum frame size (in bytes).
     */
    public int min_frame_size;
    /**
     * The maximum frame size (in bytes).
     */
    public int max_frame_size;
    
    /**
     * Creates a new uniform frame size generator.
     *
     * @param fsize  average frame size (in bytes)
     * @param frange range of frame sizes (in bytes)
     */
    public UniformFrameSizeGenerator (int fsize, int frange) {
	super(fsize);
	min_frame_size = fsize - frange / 2;
	max_frame_size = fsize + frange / 2;
    }

    /**
     * Returns the next frame size.
     *
     * @return the next frame size
     */
    public int getNextFrameSize () {
	return min_frame_size + rng.nextInt(max_frame_size - min_frame_size + 1);
    }
}
