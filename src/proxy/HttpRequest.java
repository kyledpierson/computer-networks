package proxy;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * HttpRequest - HTTP request container and parser
 *
 * Author: Kyle Pierson Date: 1/27/2015 Version 1.0
 * 
 * NOTE: All exceptions thrown should be caught by the proxy and handled
 * appropriately
 */
public class HttpRequest {
	/** Help variables */
	final static String CRLF = "\r\n";
	final static int HTTP_PORT = 80;

	/** Store the request parameters */
	String method;
	String URI;
	String version;
	String headers = "";
	String ifMod = "";

	/** Server and port */
	private String host;
	private int port;

	/**
	 * Create HttpRequest by reading it from the client socket
	 * 
	 * @throws HTTPReqException
	 * 
	 */
	public HttpRequest(BufferedReader from) throws HTTPReqException {
		// Tries to read the first line from the buffered reader
		String firstLine = "";
		try {
			firstLine = from.readLine();
		} catch (IOException e) {
			throw new HTTPReqException("400 Bad Request" + CRLF);
		}

		String[] tmp = firstLine.split(" ");

		// Checks to make sure that this request is long enough
		if (tmp.length < 3) {
			throw new HTTPReqException("400 Bad Request" + CRLF);
		}

		// Sets the method, URI and version using the first line of the request
		method = tmp[0];
		URI = tmp[1];
		version = tmp[2];

		if (!method.equals("GET")) {
			throw new HTTPReqException("501 Not Implemented" + CRLF);
		}

		if (!version.startsWith("HTTP/")) {
			throw new HTTPReqException("400 Bad Request" + CRLF);
		}

		port = HTTP_PORT;

		if (URI.startsWith("http://")) {
			String[] tmp2 = URI.split("/");
			String[] absPort = tmp2[2].split(":");

			// Extract the host
			host = absPort[0];

			// And the port
			try {
				if (absPort.length > 1)
					port = Integer.parseInt(absPort[1]);
			} catch (NumberFormatException e) {
				throw new HTTPReqException("400 Bad Request" + CRLF);
			}

			if (URI.indexOf("/", 7) == -1)
				URI = "/";
			else
				URI = URI.substring(URI.indexOf("/", 7));
		}

		// Tries to read the rest of the lines from the buffered reader
		try {
			String line = from.readLine();
			while (line.length() != 0) {
				if (!line.startsWith("Connection")) {
					/*
					 * We need to find host header to know which server to
					 * contact in case the request URI is not complete.
					 */
					if (line.startsWith("Host")) {
						tmp = line.split(" ");
						if (tmp[1].indexOf(':') > 0) {
							String[] tmp2 = tmp[1].split(":");
							host = tmp2[0];
							port = Integer.parseInt(tmp2[1]);
						} else {
							host = tmp[1];
						}
					}
					// Client is using some sort of cache
					else if (line.startsWith("If-Modified-Since")) {
						String tmp2 = line.substring(line.indexOf(':') + 1);
						ifMod = tmp2.trim();
					} else {
						// Add the line to the headers string
						headers += line + CRLF;
					}
				}

				// Advance to the next line
				line = from.readLine();
			}
		} catch (IOException e) {
			throw new HTTPReqException("Error 400 Bad Request");
		}
	}

	public void addHeader(String newHeader) {
		headers += newHeader + CRLF;
	}

	/** Return host for which this request is intended */
	public String getHost() {
		return host;
	}

	/** Return port for server */
	public int getPort() {
		return port;
	}

	/**
	 * Convert request into a string for easy re-sending.
	 */
	public String toString() {
		String req = "";

		req = method + " " + URI + " " + version + CRLF;
		req += "Host: " + host + CRLF;
		req += headers;

		// This proxy does not support persistent connections
		req += "Connection: close" + CRLF;
		req += CRLF;

		return req;
	}

	public class HTTPReqException extends Exception {
		public HTTPReqException(String message) {
			super(message);
		}
	}
}