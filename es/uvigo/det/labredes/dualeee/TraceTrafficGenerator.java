package es.uvigo.det.labredes.dualeee;

import java.io.*;

/**
 * This class extends TrafficGenerator class to feed the simulator with a traced file.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public class TraceTrafficGenerator extends TrafficGenerator {
    private BufferedReader tracefile;

    /**
     * Creates a new trace traffic generator.
     *
     * @param filename name of the trace file
     * @param size frame size (in bytes)
     */
    public TraceTrafficGenerator (String filename, int size) {
	super(0, size);
	try {
	    tracefile = new BufferedReader(new FileReader(filename));
	} catch (FileNotFoundException e) {
	    DualModeEeeSimulator.printError("Trace file not found!");
	}
    }

    /**
     * Returns the instant at which the next frame arrives.
     *
     * @return instant at which the next frame arrives (in seconds)
     */
    public double getNextArrival () {
	try {
	    String line = tracefile.readLine();
	    if (line != null) {
		String[] line_fields = line.split("\\s+");
		try {
		    arrival_time += Double.parseDouble(line_fields[0]);
		} catch (NumberFormatException e) {
		    DualModeEeeSimulator.printError("Trace file: invalid interarrival time!");
		}
	    } else {
		arrival_time = DualModeEeeSimulator.simulation_length + 1;
	    }
	} catch (IOException e) {
	    DualModeEeeSimulator.printError("Error while reading trace file!");
	}
	return arrival_time;
    }
}
