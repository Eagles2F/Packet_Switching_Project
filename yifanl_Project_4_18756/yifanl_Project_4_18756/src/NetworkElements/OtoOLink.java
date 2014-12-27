/**
 * @author Andrew Fox
 */
package NetworkElements;

import DataTypes.*;

public class OtoOLink {
	private NIC r1NIC=null, r2NIC=null;
	private Boolean trace=false;

	/**
	 * The default constructor for a OtoOLink
	 * @param computerNIC
	 * @param routerNIC
	 * @since 1.0
	 */
	public OtoOLink(NIC r1NIC, NIC r2NIC){	
		this.r1NIC = r1NIC;
		this.r1NIC.connectOtoOLink(this);
		this.r2NIC = r2NIC;
		this.r2NIC.connectOtoOLink(this);

		if(this.trace){
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R1 nic is null");
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R2 nic is null");
		}
	}

	/**
	 * Sends a packet from one end of the link to the other
	 * @param packet the packet to be sent
	 * @param nic the nic the packet is being sent from
	 * @since 1.0
	 */
	public void sendPacket(Packet packet, NIC nic){
		if(this.r1NIC.equals(nic)){
			if(this.trace)
				System.out.println("(OtoOLink) Trace: sending packet from router A to router B");

			this.r2NIC.receivePacket(packet);
		}
		else if(this.r2NIC.equals(nic)){
			if(this.trace)
				System.out.println("(OtoOLink) Trace: sending packet from router B to router A");

			this.r1NIC.receivePacket(packet);
		}
		else
			System.err.println("(OtoOLink) Error: You are trying to send a packet down a link that you are not connected to");
	}
}
