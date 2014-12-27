package NetworkElements;

import DataTypes.*;

import java.util.*;

public class LSRNIC {
	private LSR parent; // The router or computer that this nic is in
	private OtoOLink link; // The link connected to this nic

	private boolean trace = false; // should we print out debug statements?
	private ArrayList<Packet> inputBuffer = new ArrayList<Packet>(); // Where packets are put between the parent and nic
	private ArrayList<Packet> outputBuffer = new ArrayList<Packet>(); // Where packets are put to be sent
	private int linerate = 50;
	/**
	 * Default constructor for an ATM NIC
	 * @param parent
	 * @since 1.0
	 */
	public LSRNIC(LSR parent){
		this.parent = parent;
		this.parent.addNIC(this);
	}
	
	/**
	 * This method is called when a packet is passed to this nic to be sent. The packet is placed
	 * in an output buffer until a time unit passes
	 * @param currentPacket the packet to be sent (placed in the buffer)
	 * @param parent the router the packet came from
	 * @since 1.0
	 */
	public void sendPacket(Packet currentPacket, LSR parent){
		if(this.trace){
			System.out.println("Trace (LSR NIC): Received packet");
			if(this.link==null)
				System.out.println("Error (LSR NIC): You are trying to send a packet through a nic not connected to anything");
			if(this.parent!=parent)
				System.out.println("Error (LSR NIC): You are sending data through a nic that this router is not connected to");
			if(currentPacket==null)
				System.out.println("Warning (LSR NIC): You are sending a null packet");
		}
		
		this.outputBuffer.add(currentPacket);
		
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
	 * @param currentPacket the packet that was received
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket){
		this.inputBuffer.add(currentPacket);

	}
	
	/**
	 * Moves the packets from the output buffer to the line (then they get moved to the next nic's input buffer)
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.outputBuffer.size(); i++)
			this.link.sendPacket(this.outputBuffer.get(i), this);
		
		this.outputBuffer.clear();
	}
	
	/**
	 * Moves packets from this nics input buffer to its output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.inputBuffer.size(); i++)
			this.parent.receivePacket(this.inputBuffer.get(i), this);
		this.inputBuffer.clear();
	}
	
	public OtoOLink getLink() {
		return link;
	}

	public void setLink(OtoOLink link) {
		this.link = link;
	}
	public LSR getParent() {
		return parent;
	}

	public void setParent(LSR parent) {
		this.parent = parent;
	}
}
