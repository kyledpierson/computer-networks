--------------------------------------------------------------------------------------------------------------------------------------------------------

Author:  Kyle Pierson
Project: Secure Messaging Clients
Date:    1 May 2015
Version: 1.0

--------------------------------------------------------------------------------------------------------------------------------------------------------

RUN BOB FIRST!  Bob acts as the server.

Command line arguments:
Bob:   <port> <option>
Alice: <-ip-> <-port-> <option>

Where the option is "-v" if debug statements are desired

For testing, I used port 2118, which was open and free to use, as well as two machines in the CADE lab (lab2-3 and lab2-2).

This tarball should include the tars alice.tar and bob.tar, which can be imported into eclipse or otherwise extracted.

--------------------------------------------------------------------------------------------------------------------------------------------------------

To run the program, I used two workbenches of eclipse on different computers.  I started Bob on the first one, and then Alice on the second one.

Bob takes in two command line arguments of the port to listen on, and -v if debug statements are wanted.
Alice takes in three command line arguments, the IP address and port on which to connnect, and -v if debug statements are desired.

Once the program runs, Alice and Bob send each other the public keys that the other side needs to meet the preconditions of the assignment.  Alice then
allows the user to input a message to Bob.  On pressing "Enter", the program flow begins as explained in the specs.

--------------------------------------------------------------------------------------------------------------------------------------------------------

You can also run the two java files from the linux command line on two separate computers.  Just compile the Bob and Communicate classes on one
computer using the javac command, and then run Communicate (since Communicate has the main method).  Then do the same for Alice on another computer,
and the program starts.

--------------------------------------------------------------------------------------------------------------------------------------------------------
