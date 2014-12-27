/**
 * @author Andrew Fox
 */
package NetworkElements;

import java.util.*;

import DataTypes.Packet;

public class Switch implements PacketConsumer{
	ArrayList<NIC> inputNICs=new ArrayList<NIC>();		// NICs from input side
	ArrayList<NIC> outputNICs=new ArrayList<NIC>();		// NICs on output side
	boolean inputQueue=false;							// if the switch is input queued or output queued

	public Switch (int numComputers){
		for (int i=0;i<numComputers;i++){
			NIC nic1=new NIC(this,0);
			NIC nic2=new NIC(this,1);
		}
	}

	/**
	 * Adds a nic to the router
	 * side - whether it is on the source or destination side
	 */
	public void addNIC(NIC nic, int side){
		if(side==0)
			inputNICs.add(nic);
		if(side==1)
			outputNICs.add(nic);
	}

	// NOT CALLED
	public void addNIC(NIC nic){}


	/**
	 * Returns an array of NICs on source side
	 * @return
	 */
	public ArrayList<NIC> getInputNICs(){
		return this.inputNICs;
	}

	/**
	 * Returns an array of NICs on destination side
	 * @return
	 */
	public ArrayList<NIC> getOutputNICs(){
		return this.outputNICs;
	}

	/**
	 * Sets the size of the buffers in the NICs on the switch
	 */
	public void setSwitchBufferSize(){
		if(inputQueue==true){
			for(NIC nic:inputNICs){

			}
			for(NIC nic:outputNICs){

			}
		}
		else if(inputQueue==false){
			for(NIC nic:inputNICs){

			}
			for(NIC nic:outputNICs){

			}
		}
	}


	/**
	 * Sends packets from the queues on the source
	 * side of the switch to the destination side
	 */
	public void sendFromBuffer(){
		if(inputQueue==true){

		}
		else if(inputQueue==false){

		}
	}

	/**
	 * Sets if the switch is input queued
	 */
	public void setInputQueue(){
		inputQueue=true;
	}

	/**
	 * Sets if the switch is output queued
	 */
	public void setOutputQueue(){
		inputQueue=false;
	}


	/**
	 * Sends packets from the destination side to their final computer destination
	 */
	public void sendFromOutputs(){
		for(NIC nic:outputNICs){
			nic.sendFromBuffer();
		}
	}
}
