/**
 * @author Andrew Fox
 */

package DataTypes;

import java.net.*;

public class Packet {
	private int source; 
	private int dest; // The source and destination addresses
	private int delay=0; // The delay the packet has experienced

	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 */
	public Packet(int source, int dest){
		try{
			this.source = source;
			this.dest = dest;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}


	/**
	 * Returns the source ip address of this packet
	 * @return the source ip address of this packet
	 * @since 1.0
	 */
	public int getSource(){
		return this.source;
	}

	/**
	 * Returns the destination ip address of this packet
	 * @return the destination ip address of this packet
	 * @since 1.0
	 */
	public int getDest(){
		return this.dest;
	}

	/**
	 * Adds delay to this packet
	 * @param delay the amount of delay this packet has experienced
	 * @since 1.0
	 */
	public void addDelay(int delay){
		this.delay +=delay;
	}

	/**
	 * Returns the delay this packet has experienced
	 * @return the total delay of this packet
	 * @since 1.0
	 */
	public int getDelay(){
		return this.delay;
	}

	/**
	 * Returns this packet as a String
	 * @return the string version of this packet
	 * @since 1.0
	 */
	public String toString(){
		return source + " > " + dest + " took " + this.getDelay() + " time"; 
	}


}
