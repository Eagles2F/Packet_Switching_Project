/**
 * @author Andrew Fox
 */
package NetworkElements;

import DataTypes.*;
import java.util.*;

public class NIC {
	private PacketConsumer parent; // The switch or computer that this nic is in
	private OtoOLink link; // The link connected to this nic
	private boolean trace = true; // should we print out debug statements?
	private ArrayList<Packet> buffer = new ArrayList<Packet>(); // Where packets are put between the parent and nic
	private int bufferSize = 20; // the maximum number of packets allowed in the buffer


	/**
	 * Default constructor for an NIC
	 * @param parent
	 * @since 1.0
	 */
	public NIC(PacketConsumer parent){
		this.parent = parent;
		this.parent.addNIC(this);
	}

	/**
	 * Constructor for the NIC, called from Switch
	 * @param parent
	 * @param side - indicates which side of the switch (source or destination) the nic is attached to
	 */
	public NIC(PacketConsumer parent,int side){
		this.parent = parent;
		this.parent.addNIC(this,side);
	}

	/**
	 * This method is called when a packet is passed to this nic to be sent. 
	 * @param packet the packet to be sent (placed in the buffer)
	 * @param parent the place the packet came from
	 * @since 1.0
	 */
	public void sendPacket(Packet packet, PacketConsumer parent){
		if(this.trace){
			System.out.println("Trace (NIC): Received packet");
			if(this.link==null)
				System.out.println("Error (NIC): You are trying to send a cell through a nic not connected to anything");
			if(this.parent!=parent)
				System.out.println("Error (NIC): You are sending data through a nic that this router is not connected to");
			if(packet==null)
				System.out.println("Warning (NIC): You are sending a null cell");
		}

		buffer.add(packet);

	}


	/**
	 * This method connects a link to this nic
	 * @param link the link to connect to this nic
	 * @since 1.0
	 */
	public void connectOtoOLink(OtoOLink link){
		this.link = link;
	}

	/**
	 * This method is called when a packet is received over the link that this nic is connected to
	 * @param packet the packet that was received
	 * @since 1.0
	 */
	public void receivePacket(Packet packet){
		this.buffer.add(packet);
	}

	/**
	 * Sends packets from the link across the link
	 */
	public void sendFromBuffer(){
		for(int i=0; i<Math.min(1,this.buffer.size()); i++){
			this.link.sendPacket(this.buffer.get(i), this);
			this.buffer.remove(i);
		}
	}

	/**
	 * Returns the buffer of packets on the NIC
	 * @return
	 */
	public ArrayList<Packet> getBuffer(){
		return this.buffer;
	}

	/**
	 * Sets the maximum amount of packets that can be held in the buffer
	 * @param size
	 */
	public void setBufferSize(int size){
		this.bufferSize=size;
	}

	/**
	 * Returns the maximum amount of packets that can be held in the buffer
	 * @return
	 */
	public int getBufferSize(){
		return this.bufferSize;
	}



}