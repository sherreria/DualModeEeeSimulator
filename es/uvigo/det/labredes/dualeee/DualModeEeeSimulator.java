package es.uvigo.det.labredes.dualeee;

import java.io.*;

/**
 * DualModeEeeSimulator: a Java program that simulates a dual-mode EEE link.
 *
 * @author Sergio Herreria-Alonso 
 * @version 1.0
 */
public final class DualModeEeeSimulator {

    /* Simulation parameters */
    /**
     * Length of the simulation (in picoseconds). Default = 10 seconds.
     */
    public static long simulation_length = (long) 10e12;
    /**
     * Seed for the simulation. Default = 123456789.
     */
    public static long simulation_seed = 123456789;
    /**
     * If true a message for each simulated event is printed on standard output. Default = false.
     */
    public static boolean simulation_verbose = false;
    /**
     * Dual-mode EEE link
     */
    public static EeeLink link;
    /**
     * Event handler.
     */
    public static EventList event_handler;
    
    /* EEE physical parameters */
    public static long link_capacity = (long) 40e9;
    public static long active_to_fast_t = (long) (0.9e-6 * 1e12);
    public static long fast_to_deep_t = (long) (1e-6 * 1e12);
    public static long fast_to_active_t = (long) (0.34e-6 * 1e12);
    public static long deep_to_active_t = (long) (5.5e-6 * 1e12);
    public static double fast_wake_consumption = 0.7;
    public static double deep_sleep_consumption = 0.1;
    
    /* EEE configuration parameters */
    public static long target_delay = (long) (32e-6 * 1e12);
    public static long max_delay = (long) (128e-6 * 1e12);
    public static int fast_to_active_qth = 1;
    public static int deep_to_active_qth = 1;
    public static long max_fast_wake_time = (long) (3.5e-6 * 1e12);
    public static String operation_mode = "dual";
 
    private DualModeEeeSimulator () {}

    /**
     * Prints on standard error the specified message and exits.
     */
    public static void printError (String s) {
	System.err.println("ERROR: " + s);
	System.exit(1);
    }

    /**
     * Main method.
     * Usage: java DualModeEeeSimulator [-l simulation_length] [-s simulation_seed] [-f config_file] [-v]
     */
    public static void main (String[] args) {
	BufferedReader configuration_file = null;

	// Traffic parameters
	String traffic_distribution = "deterministic";
        double arrival_rate = 1e9; // in bits per second
	double alpha_pareto = 2.5; // if pareto traffic distribution
	String traffic_file = ""; // if trace simulation
	String frame_size_distribution = "deterministic";
	int frame_size = 1500; // in bytes
	int frame_size_range = 0; // if uniform frame size distribution
	String frame_size_file = ""; // if trace simulation

	// Arguments parsing
	for (int i = 0; i < args.length; i++) {
	    if (args[i].equals("-l")) {
		try {
		    simulation_length = (long) (1e12 * Double.parseDouble(args[i+1]));
		} catch (NumberFormatException e) {
		    printError("Invalid simulation length!");
		}
		i++;
	    } else if (args[i].equals("-s")) {
		try {
		    simulation_seed = Integer.parseInt(args[i+1]);
		} catch (NumberFormatException e) {
		    printError("Invalid simulation seed!");
		}
		i++;
	    } else if (args[i].equals("-f")) {
		try {
		    configuration_file = new BufferedReader(new FileReader(args[i+1]));
		} catch (FileNotFoundException e) {
		    printError("Config file not found!");
		}
		i++;
	    } else if (args[i].equals("-v")) {
                simulation_verbose = true;
	    } else {
		printError("Unknown argument: " + args[i] + "\nUsage: java DualModeEeeSimulator [-l simulation_length] [-s simulation_seed] [-f config_file] [-v]");
	    }
	}

	// Configuration file parsing
	try {
	    for (String line; (line = configuration_file.readLine()) != null;) {
		if (line.startsWith(";")) { // Just a comment
		    continue;
		}
		String[] line_fields = line.split("\\s+");
		if (line_fields[0].equals("LINK")) {
		    try {
			link_capacity = (long) Double.parseDouble(line_fields[1]);
		    } catch (NumberFormatException e) {
			printError("Config file: invalid link capacity!");
		    }			
		} else if (line_fields[0].equals("TRAFFIC")) {
		    if (line_fields[1].matches("deterministic|poisson|pareto|trace")) {
			traffic_distribution = line_fields[1];
		    } else {
			printError("Config file: invalid traffic distribution!");
		    }
		    int field_offset = 0;
		    if (traffic_distribution.equals("trace")) {
			traffic_file = line_fields[2];
		    } else {
			try {
			    arrival_rate = Double.parseDouble(line_fields[2]);
			} catch (NumberFormatException e) {
			    printError("Config file: invalid arrival rate!");
			}
			if (traffic_distribution.equals("pareto")) {
			    try {
				alpha_pareto = Double.parseDouble(line_fields[3]);
			    } catch (NumberFormatException e) {
				printError("Config file: invalid alpha pareto parameter!");
			    }
			    field_offset = 1;
			}
		    }
		    if (line_fields[3 + field_offset].matches("deterministic|uniform|trace")) {
			frame_size_distribution = line_fields[3 + field_offset];
		    } else {
			printError("Config file: invalid frame size distribution!");
		    }
		    if (frame_size_distribution.equals("trace")) {
			frame_size_file = line_fields[4 + field_offset];
		    } else {
			try {
			    frame_size = Integer.parseInt(line_fields[4 + field_offset]);
			} catch (NumberFormatException e) {
			    printError("Config file: invalid frame size!");
			}
			if (frame_size <= 0) {
			    printError("Config file: invalid frame size!");
			}
			if (frame_size_distribution.equals("uniform")) {
			    try {
				frame_size_range = Integer.parseInt(line_fields[5 + field_offset]);
			    } catch (NumberFormatException e) {
				printError("Config file: invalid frame size range!");
			    }
			    if (frame_size_range <= 0 || frame_size - frame_size_range/2.0 <= 0) {
				printError("Config file: invalid frame size range!");
			    }
			}
		    }
		} else if (line_fields[0].equals("FAST")) {
		    try {
			fast_wake_consumption = Double.parseDouble(line_fields[1]);
			active_to_fast_t = (long) (1e12 * Double.parseDouble(line_fields[2]));
			fast_to_active_t = (long) (1e12 * Double.parseDouble(line_fields[3]));
		    } catch (NumberFormatException e) {
			printError("Config file: invalid fast wake configuration!");
		    }
		} else if (line_fields[0].equals("DEEP")) {
		    try {
			deep_sleep_consumption = Double.parseDouble(line_fields[1]);
			fast_to_deep_t = (long) (1e12 * Double.parseDouble(line_fields[2]));
			deep_to_active_t = (long) (1e12 * Double.parseDouble(line_fields[3]));
		    } catch (NumberFormatException e) {
			printError("Config file: invalid deep sleep configuration!");
		    }
		} else if (line_fields[0].equals("EEE")) {
		    if (line_fields[1].matches("dual|fast|deep|dual_dyn|fast_dyn|deep_dyn|fast_mul|deep_mul|fast_time_dyn|deep_time_dyn|mostowfi")) {
			operation_mode = line_fields[1];
		    } else {
			printError("Config file: invalid EEE operation mode!");
		    }
		    try {
			target_delay = (long) (1e12 * Double.parseDouble(line_fields[2]));
			max_delay = (long) (1e12 * Double.parseDouble(line_fields[3]));
			fast_to_active_qth = Integer.parseInt(line_fields[4]);
			deep_to_active_qth = Integer.parseInt(line_fields[5]);
			max_fast_wake_time = (long) (1e12 * Double.parseDouble(line_fields[6]));
		    } catch (NumberFormatException e) {
			printError("Config file: invalid EEE configuration!");
		    }
		    if (deep_to_active_t < fast_to_active_t) {
			printError("Config file: too low deep to active transition!");
		    }
		    if ((operation_mode.matches("deep_dyn|deep_time_dyn") && target_delay < deep_to_active_t / 2.0) ||
			(operation_mode.matches("fast_dyn|fast_time_dyn|dual_dyn") && target_delay < fast_to_active_t / 2.0)) {
			printError("Config file: too low target delay!");
		    }
		    if (max_delay != 0 && (max_delay < max_fast_wake_time || max_delay < target_delay)) {
			printError("Config file: too low max delay!");
		    }
		    if (deep_to_active_qth < fast_to_active_qth) {
			printError("Config file: too low deep to active queue threshold!");
		    }
		}
	    }
	    configuration_file.close();
	} catch (IOException e) {
	    printError("Error while reading config file!");
	}
	
	// Event handler initialization
	event_handler = new EventList(simulation_length);

	// EEE link initialization
	TrafficGenerator tgen = null;
	if (traffic_distribution.equals("deterministic")) {
	    tgen = new DeterministicTrafficGenerator(arrival_rate, frame_size);
	} else if (traffic_distribution.equals("poisson")) {
	    tgen = new PoissonTrafficGenerator(arrival_rate, frame_size);
	} else if (traffic_distribution.equals("pareto")) {
	    tgen = new ParetoTrafficGenerator(arrival_rate, frame_size, alpha_pareto);
	} else if (traffic_distribution.equals("trace")) {
	    tgen = new TraceTrafficGenerator(traffic_file);
	}
	tgen.setSeed(simulation_seed);

	FrameSizeGenerator fsgen = null;
        if (frame_size_distribution.equals("deterministic")) {
            fsgen = new DeterministicFrameSizeGenerator(frame_size);
        } else if (frame_size_distribution.equals("uniform")) {
	    fsgen = new UniformFrameSizeGenerator(frame_size, frame_size_range);
        } else if (frame_size_distribution.equals("trace")) {
	    fsgen = new TraceFrameSizeGenerator(frame_size_file);
        }      
        fsgen.setSeed(simulation_seed + 1);

	link = new EeeLink(link_capacity, tgen, fsgen);	

	// Events processing
	Event event;
        while ((event = event_handler.getNextEvent(true)) != null) {
	    event_handler.handleEvent(event);
	}

	// Print statistics
	link.printStatistics();
    }
}
