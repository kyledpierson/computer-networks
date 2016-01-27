package proxy;

import java.net.Socket;

/**
 * A class used for running client sockets on different threads
 */
public class ClientHandler implements Runnable {
	private Socket client;

	/**
	 * Constructor for the client handler
	 */
	public ClientHandler(Socket _client) {
		try {
			// Set the client
			client = _client;
		} catch (NullPointerException e) {
		}
	}

	/**
	 * Called by using thread.start()
	 */
	@Override
	public void run() {
		try {
			// Handle the client
			ProxyCache.handle(client);
		} catch (Exception e) {
		}
	}
}
