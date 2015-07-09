# DualModeEeeSimulator
A Java program that simulates a dual-mode EEE link.

# Invocation
java DualModeEeeSimulator [-l simulation_length] [-s simulation_seed] [-f config_file] [-v]

# Output
The simulator outputs a summary of the link statistics:

    - Number of frames received, sent and dropped

    - Average and maximum frame delay (in useconds)

    - Time in each power state (in useconds)

    - Average power consumption (% of peak)

With option -v, the simulator outputs a line for every simulated event:

    `event_time event_type event_info`

# Legal
Copyright ⓒ Sergio Herrería Alonso <sha@det.uvigo.es> 2015

This simulator is licensed under the GNU General Public License, version 3 (GPL-3.0). For more information see LICENSE.txt
