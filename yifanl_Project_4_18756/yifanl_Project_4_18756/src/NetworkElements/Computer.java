/**
 * @author Andrew Fox
 */
package NetworkElements;

import DataTypes.*;

public class Computer implements PacketConsumer{
	private NIC nic=null;
	private Boolean trace=false;
	private int traceID = (int) (Math.random() * 100000);
	private int address=0;



	/**
	 * The default constructor for a computer
	 * @since 1.2
	 */
	public Computer(int address){
		this.address = address;
	}


	/**
	 * Sends a packet from this computer to another ip address
	 * @param dest the destination address of the packet
	 */
	public void sendPacket(int destination){
		nic.sendPacket(new Packet(address,destination), this);
	}

	/**
	 * adds a nic to this computer
	 * @param nic the nic to be added (only one nic per computer)
	 * @since 1.0
	 */
	public void addNIC(NIC nic){
		this.nic = nic;
	}

	// NOT CALLED
	public void addNIC(NIC nic, int side){}

	/**
	 * Sends cells from the buffer
	 * @since 1.0
	 */
	public void sendFromBuffer(){
		this.nic.sendFromBuffer();
	}


	/**
	 * Clears the buffer
	 * Used when packets arrive at their destination
	 */
	public void clearBuffer(){
		for (Packet p:this.nic.getBuffer()){
			if(trace)System.out.println("Received packet from "+p.getSource()+" at "+this.address+" with delay "+p.getDelay());
		}
		this.nic.getBuffer().clear();
	}


	/**
	 * This method returns a sequentially increasing random trace ID, so that we can
	 * differentiate cells in the network
	 * @return the trace id for the next cell
	 * @since 1.0
	 */
	public int getTraceID(){
		int ret = this.traceID;
		this.traceID++;
		return ret;
	}

	/**
	 * Returns the NIC attached to the computer
	 * @return
	 */
	public NIC getNIC(){
		return this.nic;
	}



}
