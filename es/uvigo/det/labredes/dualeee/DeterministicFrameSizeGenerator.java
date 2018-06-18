package es.uvigo.det.labredes.dualeee;

/**
 * This class extends FrameSizeGenerator class to simulate deterministic frame sizes.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class DeterministicFrameSizeGenerator extends FrameSizeGenerator {
    /**
     * Creates a new deterministic frame size generator.
     *
     * @param fsize average frame size (in bytes)
     */
    public DeterministicFrameSizeGenerator (int fsize) {
	super(fsize);
    }

    /**
     * Returns the next frame size.
     *
     * @return the next frame size
     */
    public int getNextFrameSize () {
	return frame_size;
    }
}
