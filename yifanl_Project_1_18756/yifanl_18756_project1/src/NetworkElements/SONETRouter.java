package NetworkElements;

import DataTypes.*;
import java.util.*;

public class SONETRouter extends SONETRouterTA{
	/**
	 * Construct a new SONET router with a given address
	 * @param	address the address of the new SONET router
	 */
	public SONETRouter(String address){
		super(address);
	}
	
	/**
	 * This method processes a frame when it is received from any location (including being created on this router
	 * from the source method). It either drops the frame from the line, or forwards it around the ring
	 * @param	frame the SONET frame to be processed
	 * @param	wavelength the wavelength the frame was received on
	 * @param	nic the NIC the frame was received on
	 */
	public void receiveFrame(SONETFrame frame, int wavelength, OpticalNICTA nic){
		if(this.dropFrequency.contains(wavelength)){// if the wavelength is one of the router's drop frequency
			if(this.destinationFrequencies.containsValue(wavelength)){//if the wavelength is one of the router's destination frequency
				// sink the signal that is from work line
				if(nic.getIsWorkingNIC() == true)  //if the nic is a workingNIC
				{
					if(nic.getHasError() != true){// if the link is OK,sink the frame from the working line 
						this.sink(frame, wavelength);
					}
				}else{// if the nic is a protectingNIC
					if(nic.getWorkingNIC().getHasError()==true){ //sink the frame from protection line only when the workingNIC has been cut.
						this.sink(frame, wavelength);
					}
				}
			}
			
		}else{// send the signal away if it is not on the drop frequency
			if(this.destinationFrequencies.containsValue(wavelength)){// send the frame only when the wavelength is in the destination frequencies
				this.sendRingFrame(frame, wavelength, nic);
			}
		}
		
	}

	
	/**
	 * Sends a frame out onto the ring that this SONET router is joined to
	 * @param	frame the frame to be sent
	 * @param	wavelength the wavelength to send the frame on
	 * @param	nic the wavelength this frame originally came from (as we don't want to send it back to the sender)
	 */
	public void sendRingFrame(SONETFrame frame, int wavelength, OpticalNICTA nic){
		// Loop through the interfaces sending the frame on interfaces that are on the ring
		// except the one it was received on. 
		for(OpticalNICTA NIC:NICs){
			// in BLSR, the frame will only sent to the NIC in the same mode with the original one
			if(nic != null){// if the frame is not from the original null interface
				if(NIC.getIsOnRing() && !NIC.equals(nic) && NIC.getIsWorkingNIC().equals(nic.getIsWorkingNIC())){
					if(NIC.getHasError()==false){ // send only to the NIC that has no error
						NIC.sendFrame(new SONETFrame(frame.getSPE().clone()), wavelength);
					}
				}
			}else{
				if(NIC.getIsOnRing() && !NIC.equals(nic)){
					if(NIC.getHasError()==false){ // send only to the NIC that has no error
						NIC.sendFrame(new SONETFrame(frame.getSPE().clone()), wavelength);
					}
				}
			}
		}
	}
}