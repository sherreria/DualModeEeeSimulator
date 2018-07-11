package es.uvigo.det.labredes.dualeee;

/**
 * This class extends FrameSizeGenerator class to simulate bimodal distributed frame sizes.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class BimodalFrameSizeGenerator extends FrameSizeGenerator {
    /**
     * The size of short frames (in bytes).
     */
    public int short_frame_size;
    /**
     * The size of long frames (in bytes).
     */
    public int long_frame_size;
    /**
     * The ratio of long frames.
     */
    public double long_frame_ratio;
    
    /**
     * Creates a new bimodal frame size generator.
     *
     * @param fsize average frame size (in bytes)
     */
    public BimodalFrameSizeGenerator (int fsize) {
	super(fsize);
	short_frame_size = 100;
	long_frame_size = 1500;
	long_frame_ratio = (fsize - short_frame_size) * 1.0 / (long_frame_size - short_frame_size);
    }

    /**
     * Returns the next frame size.
     *
     * @return the next frame size
     */
    public int getNextFrameSize () {
	if (rng.nextDouble() < long_frame_ratio) {
	    return long_frame_size;
	}
	return short_frame_size;
    }
}
