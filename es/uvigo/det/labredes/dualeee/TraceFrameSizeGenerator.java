package es.uvigo.det.labredes.dualeee;

import java.io.*;

/**
 * This class extends FrameSizeGenerator class to feed the simulator with a traced file.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class TraceFrameSizeGenerator extends FrameSizeGenerator {
    private BufferedReader tracefile;

    /**
     * Creates a new trace frame size generator.
     *
     * @param filename name of the trace file
     */
    public TraceFrameSizeGenerator (String filename) {
	super(0);
	try {
	    tracefile = new BufferedReader(new FileReader(filename));
	} catch (FileNotFoundException e) {
	    DualModeEeeSimulator.printError("Trace file not found!");
	}
    }

    /**
     * Returns the next frame size.
     *
     * @return the next frame size.
     */
    public int getNextFrameSize () {
	int next_frame_size = 0;
	try {
	    String line = tracefile.readLine();
	    if (line != null) {
		String[] line_fields = line.split("\\s+");
		try {
		    next_frame_size = Integer.parseInt(line_fields[0]);
		} catch (NumberFormatException e) {
		    DualModeEeeSimulator.printError("Trace file: invalid frame size!");
		}
	    }
	} catch (IOException e) {
	    DualModeEeeSimulator.printError("Error while reading trace file!");
	}
	return next_frame_size;
    }
}
