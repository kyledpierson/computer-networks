package go_back_n;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class StudentNetworkSimulator extends NetworkSimulator {
	/*
	 * Predefined Constants (static member variables):
	 * 
	 * int MAXDATASIZE : the maximum size of the Message data and Packet payload
	 * int A : a predefined integer that represents entity A int B : a
	 * predefined integer that represents entity B
	 * 
	 * Predefined Member Methods:
	 * 
	 * void stopTimer(int entity): Stops the timer running at "entity" [A or B]
	 * 
	 * void startTimer(int entity, double increment): Starts a timer running at
	 * "entity" [A or B], which will expire in "increment" time units, causing
	 * the interrupt handler to be called. You should only call this with A.
	 * 
	 * void toLayer3(int callingEntity, Packet p) Puts the packet "p" into the
	 * network from "callingEntity" [A or B]
	 * 
	 * void toLayer5(int entity, String dataSent) Passes "dataSent" up to layer
	 * 5 from "entity" [A or B]
	 * 
	 * double getTime() Returns the current time in the simulator. Might be
	 * useful for debugging.
	 * 
	 * void printEventList() Prints the current event list to stdout. Might be
	 * useful for debugging, but probably not.
	 * 
	 * 
	 * Predefined Classes:
	 * 
	 * Message: Used to encapsulate a message coming from layer 5
	 * 
	 * Constructor: Message(String inputData): creates a new Message containing
	 * "inputData"
	 * 
	 * Methods: boolean setData(String inputData): sets an existing Message's
	 * data to "inputData" returns true on success, false otherwise String
	 * getData(): returns the data contained in the message
	 * 
	 * Packet: Used to encapsulate a packet
	 * 
	 * Constructors: Packet (Packet p): creates a new Packet that is a copy of
	 * "p"
	 * 
	 * Packet (int seq, int ack, int check, String newPayload) creates a new
	 * Packet with a sequence field of "seq", an ack field of "ack", a checksum
	 * field of "check", and a payload of "newPayload"
	 * 
	 * Packet (int seq, int ack, int check) create a new Packet with a sequence
	 * field of "seq", an ack field of "ack", a checksum field of "check", and
	 * an empty payload
	 * 
	 * Methods: boolean setSeqnum(int n) sets the Packet's sequence field to "n"
	 * returns true on success, false otherwise
	 * 
	 * boolean setAcknum(int n) sets the Packet's ack field to "n" returns true
	 * on success, false otherwise
	 * 
	 * boolean setChecksum(int n) sets the Packet's checksum to "n" returns true
	 * on success, false otherwise
	 * 
	 * boolean setPayload(String newPayload) sets the Packet's payload to
	 * "newPayload" returns true on success, false otherwise
	 * 
	 * int getSeqnum() returns the contents of the Packet's sequence field int
	 * getAcknum() returns the contents of the Packet's ack field int
	 * getChecksum() returns the checksum of the Packet String getPayload()
	 * returns the Packet's payload
	 */

	// Add any necessary class variables here. Remember, you cannot use these
	// variables to send messages error free! They can only hold state
	// information for A or B. Also add any necessary methods (e.g. checksum of
	// a String)

	// Sender variables
	private int base;
	private int seqnum;
	private final int WINSIZE = 8;

	// Receiver variables
	private int expSeqnum;

	// Buffer of packets (includes the window)
	private List<Packet> buffer;
	private Packet receivePacket;

	// Integers for the statistics
	private int sent;
	private int recACK;
	private int corruptACK;
	private int recGoodACK;
	private int duplicateACK;

	private int ignored;
	private int retrans;
	private int buffered;

	private int sentACK;
	private int corrupt;
	private int rec;
	private int outOfOrder;

	private Map<Integer, Double> startTimes;
	private Map<Integer, Double> times;

	// This is the constructor. Don't touch!
	public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace,
			long seed) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
	}

	// This routine will be called whenever the upper layer at the sender [A]
	// has a message to send. It is the job of your protocol to ensure that the
	// data in such a message is delivered in-order, and correctly, to the
	// receiving upper layer.
	protected void aOutput(Message message) {
		// If we can buffer this message
		// NOTE: this was removed due to extreme tests that cause the buffer to
		// fill up
		// (such as testing a very large number of packets with a very small
		// time between
		// each one.
		// if ((buffer.size() - (base + WINSIZE)) < 100)
		// {
		// Get the data and compute the checksum
		String data = message.getData();
		int checksum = computeChecksum(seqnum, data);

		// Make the packet and add it to the buffer
		Packet sendPacket = new Packet(seqnum, 0, checksum, data);
		buffer.add(sendPacket);

		if (seqnum < (base + WINSIZE)) {
			// System.out.println("Sending packet " + seqnum);

			// Another packet has been sent
			sent++;

			// Send the packet
			toLayer3(A, sendPacket);
			startTimes.put(seqnum, getTime());

			// Start the timer
			if (base == seqnum) {
				// stopTimer(A);
				startTimer(A, 100);
			}
		} else {
			// System.out.println("Buffering packet " + seqnum);
			buffered++;
		}

		// Update the next seqnum
		seqnum++;
		// }
		// else
		// {
		// // This send call is ignored
		// System.out.println("Send call ignored");
		// ignored++;
		// }
	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by a B-side procedure)
	// arrives at the A-side. "packet" is the (possibly corrupted) packet
	// sent from the B-side.
	protected void aInput(Packet packet) {
		double endTime = getTime();
		// An ACK has been received
		recACK++;

		// Check to see if corrupt (if so, return)
		int acknum = packet.getAcknum();
		int check = computeChecksum(acknum, "ACK");
		if (packet.getChecksum() != check) {
			// A corrupt ACK has been received
			// System.out.println("Corrupt ACK " + acknum);
			corruptACK++;
			return;
		}

		// If this is a useful ACK
		if (base <= acknum) {
			// Stop the timer
			stopTimer(A);

			// A good ACK has been received
			recGoodACK++;

			double startTime = startTimes.get(acknum);
			times.put(acknum, endTime - startTime);

			// Update the base and send buffered messages
			while (base <= acknum) {
				// Send the next buffered item (if any)
				if (buffer.size() > base + WINSIZE) {
					sent++;
					toLayer3(A, buffer.get(base + WINSIZE));
					startTimes.put(base + WINSIZE, getTime());
				}

				base++;
			}

			// System.out.println("Received ACK " + acknum + ": base is now "
			// + base);

			// Start timer
			if (base != seqnum) {
				startTimer(A, 100);
			}
		} else {
			// System.out.println("Ignoring duplicate ACK " + acknum);

			// A duplicate ACK invalidates average RTT computation
			times.put(acknum, 0.0);
			duplicateACK++;
		}
	}

	// This routine will be called when A's timer expires (thus generating a
	// timer interrupt). You'll probably want to use this routine to control
	// the retransmission of packets. See startTimer() and stopTimer(), above,
	// for how the timer is started and stopped.
	protected void aTimerInterrupt() {
		// Start from the base
		// System.out.println("Timer expired");
		int num = base;

		// Retransmit all packets
		while (num < seqnum && num < (base + WINSIZE)) {
			// System.out.println("Retransmitting packet "
			// + buffer.get(num).getSeqnum());

			retrans++;
			toLayer3(A, buffer.get(num));
			startTimes.put(num, getTime());
			num++;
		}

		// Restart the timer
		startTimer(A, 100);
	}

	// This routine will be called once, before any of your other A-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity A).
	protected void aInit() {
		// The default state is set to 0, with no packet ready for delivery
		base = 0;
		seqnum = 0;
		buffer = new ArrayList<Packet>();

		// Used for statistics
		sent = 0;
		corruptACK = 0;
		recACK = 0;
		recGoodACK = 0;
		ignored = 0;
		retrans = 0;
		buffered = 0;

		times = new HashMap<Integer, Double>();
		startTimes = new HashMap<Integer, Double>();
	}

	// This routine will be called whenever a packet sent from the A-side
	// (i.e. as a result of a toLayer3() being done by an A-side procedure)
	// arrives at the B-side. "packet" is the (possibly corrupted) packet
	// sent from the A-side.
	protected void bInput(Packet packet) {
		// Increment the number of sent ACKs (an ACK will be sent under any
		// condition)
		sentACK++;

		// Compute the checksum
		String data = packet.getPayload();
		int check = computeChecksum(packet.getSeqnum(), data);

		// Make sure the packet is not corrupted
		if (packet.getChecksum() != check) {
			// System.out.println("Corrupt packet " + packet.getSeqnum()
			// + " sending ACK " + receivePacket.getAcknum());

			corrupt++;
			toLayer3(B, receivePacket);
			return;
		}

		// Make sure the sequence number and expected sequence number align
		if (packet.getSeqnum() != expSeqnum) {
			// System.out.println("Wrong packet " + packet.getSeqnum()
			// + " expecting " + expSeqnum + " sending ACK "
			// + receivePacket.getAcknum());

			outOfOrder++;
			toLayer3(B, receivePacket);
			return;
		}

		// Deliver the data
		toLayer5(B, data);

		// Send the ACK
		int checksum = computeChecksum(expSeqnum, "ACK");
		receivePacket = new Packet(0, expSeqnum, checksum, "ACK");

		// System.out.println("Received packet " + packet.getSeqnum()
		// + " sending ACK " + expSeqnum);

		toLayer3(B, receivePacket);

		// Update the next expected sequence number
		expSeqnum++;

		// Increment correctly received packets
		rec++;
	}

	// This routine will be called once, before any of your other B-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity B).
	protected void bInit() {
		// The next expected sequence number is set to zero
		expSeqnum = 0;

		// The default packet is set to 1
		// (as if the last packet received had a sequence number of 0)
		int checksum = computeChecksum(-1, "ACK");
		receivePacket = new Packet(0, -1, checksum, "ACK");

		sentACK = 0;
		corrupt = 0;
		outOfOrder = 0;
		rec = 0;
	}

	/**
	 * Used to compute the checksum, given a value and a string
	 * 
	 * @param check
	 * @param data
	 * @return the checksum
	 */
	private int computeChecksum(int check, String data) {
		// Add byte values to the original number
		for (int i = 0; i < data.length(); i++) {
			check += data.charAt(i);
		}

		return check;
	}

	/**
	 * Gets the total number of packet sent
	 * 
	 * @return
	 */
	public int getSent() {
		return sent;
	}

	/**
	 * Gets the total number of corrupted ACKs received by the sender
	 * 
	 * @return
	 */
	public int getCorruptACK() {
		return corruptACK;
	}

	/**
	 * Gets the total number of ACKs received by the sender
	 * 
	 * @return
	 */
	public int getRecACK() {
		return recACK;
	}

	/**
	 * Gets the total number of good ACKs received by the sender
	 * 
	 * @return
	 */
	public int getRecGoodACK() {
		return recGoodACK;
	}

	/**
	 * Gets the number of calls from layer 5 ignored by the sender
	 * 
	 * @return
	 */
	public int getIgnored() {
		return ignored;
	}

	/**
	 * Gets the total number of times the timer expires on the sender
	 * 
	 * @return
	 */
	public int getRetrans() {
		return retrans;
	}

	/**
	 * Gets the total number of ACKs sent
	 * 
	 * @return
	 */
	public int getSentACK() {
		return sentACK;
	}

	/**
	 * Gets the total number of corrupted packets received by the receiver
	 * 
	 * @return
	 */
	public int getCorrupt() {
		return corrupt;
	}

	/**
	 * Gets the total number of packets received by the receiver
	 * 
	 * @return
	 */
	public int getRec() {
		return rec;
	}

	/**
	 * Gets the total number of out-of-order packets received by the receiver
	 * 
	 * @return
	 */
	public int getBuffered() {
		return buffered;
	}

	/**
	 * Gets the total number of out-of-order packets received by the receiver
	 * 
	 * @return
	 */
	public int getOutOfOrder() {
		return outOfOrder;
	}

	/**
	 * Gets the total number of out-of-order packets received by the receiver
	 * 
	 * @return
	 */
	public int getDuplicateACK() {
		return duplicateACK;
	}

	/**
	 * Gets the total number of out-of-order packets received by the receiver
	 * 
	 * @return
	 */
	public Map<Integer, Double> getTimes() {
		return times;
	}
}