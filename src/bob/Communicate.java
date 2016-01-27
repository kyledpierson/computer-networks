package bob;

public class Communicate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("Please provide a port as arguments");
		} else {
			int port = 0;
			boolean debug = false;

			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Please enter an integer for the port number");
				System.exit(1);
			}

			if (args.length > 1 && args[1].equals("-v"))
				debug = true;

			Bob b = new Bob(port, debug);
			b.communicate();
		}
	}
}
