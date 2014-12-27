/**
 * @author andy
 * @since 1.0
 * @version 1.2
 * @date 24-10-2008
 */

package NetworkElements;

import java.util.*;
import DataTypes.*;

public class ATMRouter implements IATMCellConsumer{
	private int address; // The AS address of this router
	private ArrayList<ATMNIC> nics = new ArrayList<ATMNIC>(); // all of the nics in this router
	private TreeMap<Integer, ATMNIC> nextHop = new TreeMap<Integer, ATMNIC>(); // a map of which interface to use to get to a given router on the network
	private TreeMap<Integer, NICVCPair> VCtoVC = new TreeMap<Integer, NICVCPair>(); // a map of input VC to output nic and new VC number
	private boolean trace=false; // should we print out debug code?
	private int traceID = (int) (Math.random() * 100000); // create a random trace id for cells
	private ATMNIC currentConnAttemptNIC = null; // The nic that is currently trying to setup a connection
	private boolean displayCommands = true; // should we output the commands that are received?
	private int vc=1;
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public ATMRouter(int address){
		this.address = address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(ATMNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic in the router
	 * @param cell the cell that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receiveCell(ATMCell cell, ATMNIC nic){
		if(trace)
			System.out.println("Trace (ATMRouter): Received a cell " + cell.getTraceID());
		
		if(cell.getIsOAM()){
			// What's OAM for? 
			if(cell.getData().startsWith("setup")){
				if(this.currentConnAttemptNIC == null){
					this.receivedSetup(cell);
					//set the current nic to the nic
					this.currentConnAttemptNIC = nic;
				
					//create and send the call processing
					ATMCell callproc=new ATMCell(cell.getVC(),"call processing",this.getTraceID());
					callproc.setIsOAM(true);
					this.sentCallProceeding(callproc);
					nic.sendCell(callproc, this);
				
				
					//get the destination
					int des = this.getIntFromEndOfString(cell.getData());
				
					//whether the cell has already arrived its destination?
					if(des == this.address){ //yes
						//create the new vc
						int vc_new =  this.vc; 
						this.vc++;					
						
						VCtoVC.put(vc_new, null);
						
						//create connect cell
						ATMCell connect = new ATMCell(vc_new,"connect "+vc_new,this.getTraceID());
						connect.setIsOAM(true);
						this.sentConnect(connect);
						nic.sendCell(connect, this);		
					}else{//no,it hasn't 
						//get the output nic
						ATMNIC output_nic = this.nextHop.get(des);
							
						//create a setup cell to send to the next hop
						ATMCell setup = new ATMCell(cell.getVC(),cell.getData(),this.getTraceID());
						setup.setIsOAM(true);
						this.sentSetup(setup);
						output_nic.sendCell(setup, this);
					}
			  }else{// there is already one nic doing the setup
				  //send the wait messsage
				  ATMCell wait = new ATMCell(cell.getVC(),"wait "+this.getIntFromEndOfString(cell.getData()),this.getTraceID());
				  wait.setIsOAM(true);
				  this.sentWait(wait);
				  nic.sendCell(wait, this);
			  }
			}else if(cell.getData().startsWith("wait")){
				this.receivedWait(cell);
				//send the setup again
				ATMCell setup_ag = new ATMCell(cell.getVC(),"setup "+this.getIntFromEndOfString(cell.getData()),this.getTraceID());
				setup_ag.setIsOAM(true);
				this.sentSetup(setup_ag);
				nic.sendCell(setup_ag, this);
			}else if(cell.getData().startsWith("call processing")){
				this.receivedCallProceeding(cell);
			}else if(cell.getData().startsWith("connect")){
				if(cell.getData().equals("connect ack")){ // receive the connect ack
					this.receiveConnectAck(cell);
				}else{ // receive the connect vc_number
					this.receivedConnect(cell);
					// send connect ack back
					ATMCell connect_ack = new ATMCell(cell.getVC(),"connect ack",this.getTraceID());
					connect_ack.setIsOAM(true);
					this.sentConnectAck(connect_ack);
					nic.sendCell(connect_ack, this);
					
					int input_vc =  this.vc; 
					this.vc++;					
				
					//put the nic_vc pair and input vc into the VCtoVC lookup table
					NICVCPair nic_vc_pair = new NICVCPair(nic,this.getIntFromEndOfString(cell.getData()));
					//System.out.println(nic_vc_pair);
					VCtoVC.put(input_vc, nic_vc_pair);
					
					//send the connect 
					ATMCell connect = new ATMCell(input_vc,"connect "+input_vc,this.getTraceID());
					connect.setIsOAM(true);
					this.sentConnect(connect);
					currentConnAttemptNIC.sendCell(connect, this);
					this.currentConnAttemptNIC = null;
				}
			}else if(cell.getData().startsWith("end")){
				if(!cell.getData().equals("end ack")){// end vc
					this.recieveEnd(cell);
					//send the end ack back
					ATMCell end_ack = new ATMCell(cell.getVC(),"end ack",this.getTraceID());
					end_ack.setIsOAM(true);
					this.sentEndAck(end_ack);
					nic.sendCell(end_ack, this);
					
					if(VCtoVC.get(cell.getVC()) != null){
						//send end to next hop
						int next_vc = VCtoVC.get(cell.getVC()).getVC();
						ATMCell end = new ATMCell(next_vc,"end "+next_vc,this.getTraceID());
						end.setIsOAM(true);
						this.sentEnd(end);
						VCtoVC.get(cell.getVC()).getNIC().sendCell(end, this);
						//tear down the vc in the VCtoVC table
						VCtoVC.remove(this.getIntFromEndOfString(cell.getData()));
					}else{ // meaning this router is the destination.
						//doing nothing.
						//tear down the vc in the VCtoVC table
						VCtoVC.remove(this.getIntFromEndOfString(cell.getData()));
					}
				}else{// end ack
					this.receivedEndAck(cell);
				}
			}
		}else{
			// find the nic and new VC number to forward the cell on
			if(VCtoVC.containsKey(cell.getVC())){
				if(VCtoVC.get(cell.getVC())  == null){
					this.cellDeadEnd(cell);
				}else{
					int des_vc = VCtoVC.get(cell.getVC()).getVC();
					int input_vc = cell.getVC();
					cell.setVC(des_vc);
					VCtoVC.get(input_vc).getNIC().sendCell(cell, this);
				}
			}else{// there is no VC avalaible for the cell
				this.cellNoVC(cell);
			}
			// otherwise the cell has nowhere to go. output to the console and drop the cell
		}		
	}
	
	/**
	 * Gets the number from the end of a string
	 * @param string the sting to try and get a number from
	 * @return the number from the end of the string, or -1 if the end of the string is not a number
	 * @since 1.0
	 */
	private int getIntFromEndOfString(String string){
		// Try getting the number from the end of the string
		try{
			String num = string.split(" ")[string.split(" ").length-1];
			return Integer.parseInt(num);
		}
		// Couldn't do it, so return -1
		catch(Exception e){
			if(trace)
				System.out.println("Could not get int from end of string");
			return -1;
		}
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
	 * Tells the router the nic to use to get towards a given router on the network
	 * @param destAddress the destination address of the ATM router
	 * @param outInterface the interface to use to connect to that router
	 * @since 1.0
	 */
	public void addNextHopInterface(int destAddress, ATMNIC outInterface){
		this.nextHop.put(destAddress, outInterface);
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void clearOutputBuffers(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).clearOutputBuffers();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void clearInputBuffers(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).clearInputBuffers();
	}
	
	/**
	 * Sets the nics in the router to use tail drop as their drop mechanism
	 * @since 1.0
	 */
	public void useTailDrop(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsTailDrop();
	}
	
	/**
	 * Sets the nics in the router to use RED as their drop mechanism
	 * @since 1.0
	 */
	public void useRED(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsRED();
	}
	
	/**
	 * Sets the nics in the router to use PPD as their drop mechanism
	 * @since 1.0
	 */
	public void usePPD(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsPPD();
	}
	
	/**
	 * Sets the nics in the router to use EPD as their drop mechanism
	 * @since 1.0
	 */
	public void useEPD(){
		for(int i=0; i<this.nics.size(); i++)
			nics.get(i).setIsEPD();
	}
	
	/**
	 * Sets if the commands should be displayed from the router in the console
	 * @param displayComments should the commands be displayed or not?
	 * @since 1.0
	 */
	public void displayCommands(boolean displayCommands){
		this.displayCommands = displayCommands;
	}
	
	/**
	 * Outputs to the console that a cell has been dropped because it reached its destination
	 * @since 1.0
	 */
	public void cellDeadEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("The cell is destined for this router (" + this.address + "), taken off network " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a cell has been dropped as no such VC exists
	 * @since 1.0
	 */
	public void cellNoVC(ATMCell cell){
		if(this.displayCommands)
		System.out.println("The cell is trying to be sent on an incorrect VC " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect message has been sent
	 * @since 1.0
	 */
	private void sentSetup(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND SETUP: Router " +this.address+ " sent a setup " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a setup message has been sent
	 * @since 1.0
	 */
	private void receivedSetup(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC SETUP: Router " +this.address+ " received a setup message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a call proceeding message has been received
	 * @since 1.0
	 */
	private void receivedCallProceeding(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC CALLPRO: Router " +this.address+ " received a call proceeding message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect message has been sent
	 * @since 1.0
	 */
	private void sentConnect(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CONN: Router " +this.address+ " sent a connect message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect message has been received
	 * @since 1.0
	 */
	private void receivedConnect(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC CONN: Router " +this.address+ " received a connect message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect ack message has been sent
	 * @since 1.0
	 * @version 1.2
	 */
	private void sentConnectAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CALLACK: Router " +this.address+ " sent a connect ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a connect ack message has been received
	 * @since 1.0
	 */
	private void receiveConnectAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC CALLACK: Router " +this.address+ " received a connect ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an call proceeding message has been received
	 * @since 1.0
	 */
	private void sentCallProceeding(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND CALLPRO: Router " +this.address+ " sent a call proceeding message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end message has been sent
	 * @since 1.0
	 */
	private void sentEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND ENDACK: Router " +this.address+ " sent an end message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end message has been received
	 * @since 1.0
	 */
	private void recieveEnd(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC ENDACK: Router " +this.address+ " received an end message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end ack message has been received
	 * @since 1.0
	 */
	private void receivedEndAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC ENDACK: Router " +this.address+ " received an end ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that an end ack message has been sent
	 * @since 1.0
	 */
	private void sentEndAck(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND ENDACK: Router " +this.address+ " sent an end ack message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a wait message has been sent
	 * @since 1.0
	 */
	private void sentWait(ATMCell cell){
		if(this.displayCommands)
		System.out.println("SND WAIT: Router " +this.address+ " sent a wait message " + cell.getTraceID());
	}
	
	/**
	 * Outputs to the console that a wait message has been received
	 * @since 1.0
	 */
	private void receivedWait(ATMCell cell){
		if(this.displayCommands)
		System.out.println("REC WAIT: Router " +this.address+ " received a wait message " + cell.getTraceID());
	}
}
