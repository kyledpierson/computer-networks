---------------------------------------------------------------------------------------------------

Author:  Kyle Pierson
Project: Caching Proxy Server
Date:    1/27/2015
Version: 1.0

---------------------------------------------------------------------------------------------------

About this program

Included in this .jar file are three .java files, with the ProxyCache including "main".  They
simulate a proxy server, which receives HTTP requests form a client, parses and validates them,
and sends them to the server.  The server then sends back the response, which is then sent back
to the client.  Only the GET method is supported for HTTP requests.  An error code of 400 is
returned upon receiving an invalid request, and an error code of 501 is received upon receiving
a request for a method other than GET.

---------------------------------------------------------------------------------------------------

Compiling and Running the Program

This program was written, compiled, and run using the Eclipse IDE.  Eclipse automatically builds
and compiles the code on the fly.  To run the code, right click on the ProxyCache class, hover
over "Run As", and click on "Run Configurations".  In the "Arguments" tab, enter in the port
number on which you want the proxy server to listen.  Then click run.  The proxy server is now
up and running and listening for connections on the port specified.

Alternatively, the files can be compiled and run from the terminal.  To compile, use the command
javac HttpRequest.java HttpResponse.java ProxyCache.java.  To run the class with main, use the
command java ProxyCache *portnumber* (where *portnumber* is the port on which you want the server
to listen.  This should run the ProxyCache which includes the main method.  The server is now up
and running and listening for connections on the port specified

---------------------------------------------------------------------------------------------------

Connecting to the server

To connect to the server, use the command:

	telnet <server_ip> <port>

where server_ip is the IP address of the server and port is the port the server is listening on.
You can now send HTTP requests to the server.

Alternatively, you can set your proxy server in firefox to be the running server

	Settings -> Options -> Advanced -> Network -> Settings -> Manual Proxy Configurations
	
Enter in the IP address and port number of the proxy server.
---------------------------------------------------------------------------------------------------

Here are some of the websites I used to test my proxy server

www.cs.utah.edu/~kobus/simple.html
www.google.com
www.msn.com
www.utah.edu
uofu-cs4540-57.cloudapp.net
www.yahoo.com

---------------------------------------------------------------------------------------------------

CACHING

For the cache, I used a map which maps host + URI keys to an integer value (which is assigned as
the size of the cache at that time).  The contents of the response are then cached at a file name
in (home_directory)/kyle_proxy_cache/(integer_value).  If a key exists in the map, then the file
exists, and it is returned to the client (if it is up to date, as described in the next section).

---------------------------------------------------------------------------------------------------

EXTRA CREDIT

I added complete functionality to the extra credit.

I saved all Last-Modified dates from responses in a map, which links the key of host + URI to a
String, which is the last modified date.  When I am checking for an object in the cache, I need
to send a conditional request to the server with an if-modifeed-since header.

I use the last modified date saved in the map as an if-modified-since date in the request. IF the
client already has included such a header, and that header is more recent, then it is used in the
request to the server instead.  THIS MAY MEAN that the cache needs to be updated, and that is
checked for when the response comes back, i.e. receiving a 304 does not guarantee that the cache
is up to date IF we used the client's if-modified-since header instead of the one stored in the
cache.

Certain sites, like google.com, do not use a Last-Modified header, and so a default current date
is set as the last-modified date.

--------------------------------------------------------------------------------------------------