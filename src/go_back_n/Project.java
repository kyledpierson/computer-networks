package go_back_n;

import java.io.*;
import java.util.Map;

public class Project {
	public final static void main(String[] argv) {
		StudentNetworkSimulator simulator;

		int nsim = -1;
		double loss = -1.0;
		double corrupt = -1.0;
		double delay = -1.0;
		int trace = -1;
		long seed = -1;
		String buffer = "";

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Network Simulator v1.0");

		while (nsim < 1) {
			System.out.print("Enter number of messages to simulate (> 0): " + "[10] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				nsim = 10;
			} else {
				try {
					nsim = Integer.parseInt(buffer);
				} catch (NumberFormatException nfe) {
					nsim = -1;
				}
			}
		}

		while (loss < 0.0) {
			System.out.print("Enter the packet loss probability (0.0 for no " + "loss): [0.0] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				loss = 0.0;
			} else {
				try {
					loss = (Double.valueOf(buffer)).doubleValue();
				} catch (NumberFormatException nfe) {
					loss = -1.0;
				}
			}
		}

		while (corrupt < 0.0) {
			System.out.print("Enter the packet corruption probability (0.0 " + "for no corruption): [0.0] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				corrupt = 0.0;
			} else {
				try {
					corrupt = (Double.valueOf(buffer)).doubleValue();
				} catch (NumberFormatException nfe) {
					corrupt = -1.0;
				}
			}
		}

		while (delay <= 0.0) {
			System.out.print("Enter the average time between messages from " + "sender's layer 5 (> 0.0): [1000] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				delay = 1000.0;
			} else {
				try {
					delay = (Double.valueOf(buffer)).doubleValue();
				} catch (NumberFormatException nfe) {
					delay = -1.0;
				}
			}
		}

		while (trace < 0) {
			System.out.print("Enter trace level (>= 0): [0] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				trace = 0;
			} else {
				try {
					trace = Integer.parseInt(buffer);
				} catch (NumberFormatException nfe) {
					trace = -1;
				}
			}
		}

		while (seed < 1) {
			System.out.print("Enter random seed: [random] ");
			try {
				buffer = stdIn.readLine();
			} catch (IOException ioe) {
				System.out.println("IOError reading your input!");
				System.exit(1);
			}

			if (buffer.equals("")) {
				seed = System.currentTimeMillis();
			} else {
				try {
					seed = (Long.valueOf(buffer)).longValue();
				} catch (NumberFormatException nfe) {
					seed = -1;
				}
			}
		}

		simulator = new StudentNetworkSimulator(nsim, loss, corrupt, delay, trace, seed);

		simulator.runSimulator();

		int totalA = simulator.getSent() + simulator.getRetrans();
		int packLost = totalA - simulator.getSentACK();
		int ackLost = simulator.getSentACK() - simulator.getRecACK();

		System.out.println("");
		System.out.println("Total number of packets sent by A: " + totalA);
		System.out.println("Number of unique packets sent: " + simulator.getSent());
		System.out.println("Number of buffered packets: " + simulator.getBuffered());
		System.out.println("Number of packets retransmitted: " + simulator.getRetrans());
		System.out.println("Number of Layer 5 calls ignored: " + simulator.getIgnored());
		System.out.println("");

		System.out.println("Number packets lost: " + packLost);
		System.out.println("Number packets corrupted: " + simulator.getCorrupt());
		System.out.println("Number of out-of-order packets received by B: " + simulator.getOutOfOrder());
		System.out.println("Number of duplicate ACKs received by A: " + simulator.getDuplicateACK());
		System.out.println("");

		System.out.println("Number ACKs sent: " + simulator.getSentACK());
		System.out.println("Number ACKs lost: " + ackLost);
		System.out.println("Number ACKs corrupted: " + simulator.getCorruptACK());
		System.out.println("");

		// Calculate the average RTT
		Map<Integer, Double> times = simulator.getTimes();
		double totalTime = 0.0;
		int count = 0;
		for (double d : times.values()) {
			if (d != 0.0) {
				totalTime += d;
				count++;
			}
		}

		System.out.println("Average RTT: " + totalTime / count);
		System.out.println("");
	}
}
