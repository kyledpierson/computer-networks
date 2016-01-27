package proxy;

import java.net.*;
import java.nio.file.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ProxyCache.java - Simple caching proxy
 *
 * Author: Kyle Pierson Date: 1/27/2015 Version 1.0
 * 
 * NOTE: handles all exceptions that have propagated from HttpRequest and
 * HttpResponse
 */
public class ProxyCache {
	final static String CRLF = "\r\n";
	final static String SEP = System.getProperty("file.separator");

	private static String cacheDir;
	private static HashMap<String, Integer> cacheMap;
	private static HashMap<String, String> modMap;

	/** Port for the proxy */
	private static int port;
	/** Socket for client connections */
	private static ServerSocket socket;

	/** Create the ProxyCache object and the socket */
	public static void init(int p) {
		port = p;
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error creating socket: " + e.getMessage());
			System.exit(-1);
		}

		cacheDir = System.getProperty("user.home");
		cacheDir += SEP + "kyle_proxy_cache";

		File file = new File(cacheDir);
		file.mkdir();

		cacheMap = new HashMap<String, Integer>();
		modMap = new HashMap<String, String>();
	}

	public static void handle(Socket client) {
		Socket server = null;
		HttpRequest request = null;
		HttpResponse response = null;
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

		/*
		 * Process request. If there are any exceptions, then simply return and
		 * end this request. This unfortunately means the client will hang for a
		 * while, until it timeouts.
		 */

		// Read request
		try {
			// Open the input stream and convert to a buffered reader
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			// Parse the request from the client
			request = new HttpRequest(fromClient);

			String key = request.getHost() + request.URI;
			Integer val = cacheMap.get(key);

			if (val != null) {
				boolean hit = false;
				boolean usingReqDate = false;

				// We have to know whether or not to use the last-modified date
				// specified by the client, or to
				// Use the one we have in the cache.
				Date reqDate;
				Date proxyDate;
				String dateString = modMap.get(key);
				try {
					reqDate = format.parse(request.ifMod);
				} catch (ParseException e) {
					reqDate = new Date(0);
				}
				try {
					proxyDate = format.parse(dateString);
				} catch (ParseException e) {
					proxyDate = new Date(10);
				}

				if (proxyDate.before(reqDate)) {
					// This means we are using the date from the request (the
					// client is using some sort of cache)
					// this is important later on, because by using this date
					// and getting a 304, we are not
					// ensured that our cache is up to date.
					request.addHeader("If-Modified-Since: " + request.ifMod);
					usingReqDate = true;
				} else {
					// Use the date from our cache to ensure that the proxy is
					// up to date
					request.addHeader("If-Modified-Since: " + dateString);
				}

				// Find out if the file has been modified
				try {
					// Open socket and write a conditional request to socket
					server = new Socket(request.getHost(), request.getPort());
					DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
					toServer.writeBytes(request.toString());
				} catch (UnknownHostException e) {
					System.out.println("Unknown host: " + request.getHost());
					try {
						// Send the appropriate error message back to the client
						DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
						backToClient.writeBytes("400 Bad Request" + CRLF);
						close(client, server);
					} catch (IOException exc) {
						System.out.println("Unable to connect back to client: " + exc.getMessage());
						close(client, server);
					}
					return;
				} catch (IOException e) {
					try {
						// Send the appropriate error message back to the client
						DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
						backToClient.writeBytes("408 Request Timeout" + CRLF);
						close(client, server);
					} catch (IOException exc) {
						System.out.println("Unable to connect back to client: " + exc.getMessage());
						close(client, server);
					}
					return;
				}

				// Read response and forward it to client
				try {
					// Open the input stream and parse the response
					DataInputStream fromServer = new DataInputStream(server.getInputStream());
					response = new HttpResponse(fromServer);
					String head = response.toString();

					// This most likely means we are up to date (unless we are
					// using the client's last-modified-date)
					if (head.contains("304 Not Modified")) {
						// If we are not using the client's request date, we are
						// good to go
						if (!usingReqDate)
							hit = true;

						else {
							// If we are using the client's request date, there
							// is no guarantee that the above header
							// applies to our cache as well, so we need to check
							// for that
							Date lastMod;
							try {
								lastMod = format.parse(response.lastMod);
							} catch (ParseException e) {
								lastMod = new Date();
							}

							// This means our cache is up to date
							if (lastMod.before(proxyDate))
								hit = true;
						}
					}
				} catch (IOException e) {
					// An error occurred writing the response to the client
					System.out.println("Error getting last-modified date from server: " + e.getMessage());
				}

				if (hit) {
					// Store the file content in a byte array
					byte[] fileContent = null;
					try {
						String fileName = cacheDir + SEP + val;
						File file = new File(fileName);
						fileContent = read(file);
					} catch (IOException e) {
						System.out.println("An error occured reading from file " + e);
						hit = false;
					}

					if (hit) {
						try {
							// Send the cached response back to the client
							DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
							backToClient.write(fileContent);
							close(client);

							System.out.println("Object " + key + " retrieved from Cache" + CRLF);
							return;
						} catch (IOException e) {
							System.out.println("Unable to connect back to client: " + e.getMessage());
							close(client);
							return;
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Error reading input from client: " + e.getMessage());
			close(client);
			return;
		} catch (HttpRequest.HTTPReqException e) {
			// This code executes when an exception propagates up through the
			// request
			try {
				// Send the appropriate error message to the client
				DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
				backToClient.writeBytes(e.getMessage());
				close(client);
			} catch (IOException exc) {
				System.out.println("Unable to connect back to client: " + exc.getMessage());
				close(client);
			}
			return;
		}

		// Send request to server
		try {
			if (response == null) {
				// Open socket and write request to socket
				server = new Socket(request.getHost(), request.getPort());
				// Open the data output stream
				DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
				// Write the request to the server
				toServer.writeBytes(request.toString());
			}
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + request.getHost());
			try {
				// Send the appropriate error message back to the client
				DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
				backToClient.writeBytes("400 Bad Request" + CRLF);
				close(client, server);
			} catch (IOException exc) {
				System.out.println("Unable to connect back to client: " + exc.getMessage());
				close(client, server);
			}
			return;
		} catch (IOException e) {
			try {
				// Send the appropriate error message back to the client
				DataOutputStream backToClient = new DataOutputStream(client.getOutputStream());
				backToClient.writeBytes("408 Request Timeout" + CRLF);
				close(client, server);
			} catch (IOException exc) {
				System.out.println("Unable to connect back to client: " + exc.getMessage());
				close(client, server);
			}
			return;
		}

		// Read response and forward it to client
		try {
			if (response == null) {
				// Open the input stream and parse the response
				DataInputStream fromServer = new DataInputStream(server.getInputStream());
				response = new HttpResponse(fromServer);
			}

			// Open the output stream
			DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

			// Get the headers and body
			byte[] buf1 = response.toString().getBytes();
			byte[] buf2 = response.body;

			// Write response to client. First headers, then body
			toClient.write(buf1);
			toClient.write(buf2);

			// Close the client and server
			close(client, server);

			String key = request.getHost() + request.URI;
			int val = cacheMap.size();

			System.out.println("Object " + key + " retrieved from Server" + CRLF);

			// Try writing the response to the cache
			String path = cacheDir + SEP + val;
			FileOutputStream toFile = null;
			try {
				toFile = new FileOutputStream(path, false);
				toFile.write(buf1);
				toFile.write(buf2);
				toFile.close();

				cacheMap.put(key, val);
				modMap.put(key, response.lastMod);
			} catch (IOException e) {
				// The object was not written to a file
				System.out.println("Object " + key + " not written to cache: " + e.getMessage());
				// Close the file
				try {
					toFile.close();
				} catch (Exception exc) {
				}
			}
		} catch (IOException e) {
			// An error occurred writing the response to the client
			System.out.println("Error writing response to client: " + e.getMessage());

			// Close the client and server
			close(client, server);
		}
	}

	/**
	 * Reads a file into a byte array Returns null on failure
	 * 
	 * @param file
	 * @return An array of bytes read from the file
	 * @throws IOException
	 */
	public static byte[] read(File file) throws IOException {
		FileInputStream in = null;
		ByteArrayOutputStream out = null;

		// Try reading the bytes of the file into the byte array output stream
		try {
			in = new FileInputStream(file);
			out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int read = 0;

			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		} catch (IOException e) {
			// This will propogate up
			throw e;
		} finally {
			// Close the file stream and the byte stream
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				out.close();
			} catch (Exception e) {
			}
		}
		return out.toByteArray();
	}

	/**
	 * Closes the specified socket
	 * 
	 * @param a
	 */
	public static void close(Socket a) {
		// Close the socket, and disregard the exception
		try {
			a.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Closes the specified sockets
	 * 
	 * @param a
	 * @param b
	 */
	public static void close(Socket a, Socket b) {
		// Close the sockets, and disregard the exceptions
		try {
			a.close();
		} catch (Exception e) {
		}
		try {
			b.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Read command line arguments and start proxy
	 */
	public static void main(String args[]) {
		int myPort = 0;

		// Make sure a port number was provided
		try {
			myPort = Integer.parseInt(args[0]);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Need port number as argument.");
			System.exit(-1);
		} catch (NumberFormatException e) {
			System.out.println("Please give port number as integer.");
			System.exit(-1);
		}

		init(myPort);

		/**
		 * Main loop. Listen for incoming connections and spawn a new thread for
		 * handling them
		 */
		Socket client = null;

		while (true) {
			try {
				// Start any new connections on a separate thread (by creating a
				// new object of a class that implements runnable)
				client = socket.accept();
				Thread t = new Thread(new ClientHandler(client));
				t.start();
			} catch (Exception e) {
				continue;
			}
		}
	}
}