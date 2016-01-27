package alice;

import java.util.Scanner;

public class Communicate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 2) {
			System.out.println("Please provide at least an IP address and a port as arguments");
		} else {
			int port = 0;
			boolean debug = false;

			try {
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Please enter an integer for the port number");
				System.exit(1);
			}

			if (args.length > 2 && args[2].equals("-v"))
				debug = true;

			Alice a = new Alice(args[0], port, debug);

			Scanner scan = new Scanner(System.in);
			System.out.println("Enter a message to Bob");

			a.send(scan.nextLine());
		}
	}
}
