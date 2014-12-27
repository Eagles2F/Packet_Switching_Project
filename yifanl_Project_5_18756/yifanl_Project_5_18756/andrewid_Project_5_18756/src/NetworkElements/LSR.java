package NetworkElements;

import java.util.*;

import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private TreeMap<Integer, NICVCPair> VCtoVC = new TreeMap<Integer, NICVCPair>(); // a map of input VC to output nic and new VC number
	private HashMap<Integer,LSRNIC> routingTable = new HashMap<Integer,LSRNIC>();
	private HashMap<Integer,Integer> destToLabelTable = new HashMap<Integer,Integer>();  // a map of destination address to the path label
	private LSRNIC currentConnAttemptNIC = null; // The nic that is currently trying to setup a path
	private int vc = 1;
	private ArrayList<Packet> tempBuffer=new ArrayList<Packet>();
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
	}
	
	/**
	 * The return the router's address
	 * @since 1.0
	 */
	public int getAddress(){
		return this.address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(LSRNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if(currentPacket.isOAM()){
			// What's OAM for? 
			if(currentPacket.getData().equals("path")){
				if(this.currentConnAttemptNIC == null){
					System.out.println("PATH: Router "+this.getAddress()+" received a Path setup command destined to "+currentPacket.getDest());
					//set the current nic to the nic
					this.currentConnAttemptNIC = nic;
			
					//get the destination
					int des = currentPacket.getDest();
			
					//whether the packet has already arrived its destination?
					if(des == this.address){ //yes
						//create the new vc
						int vc_new =  this.vc; 
						this.vc++;					
						
						VCtoVC.put(vc_new, null);
					
						//create resource reservation packet
						Packet res = new Packet(this.getAddress(),currentPacket.getSource(),currentPacket.getDSCP());
						res.setOAM(true);
						res.setData("resv "+vc_new);
						System.out.println("RESV: Router "+this.getAddress()+" sent a resource reservation command destined to "+res.getDest());
						nic.sendPacket(res, this);
						this.currentConnAttemptNIC = null;
					}else{//no,it hasn't 
						//get the output nic
						LSRNIC output_nic = this.routingTable.get(des);
					
						System.out.println("PATH: Router "+this.getAddress()+" sent a Path setup command destined to "+currentPacket.getDest());
						//pass the path packet to the next hop
						output_nic.sendPacket(currentPacket, this);
					}
				}else{// there is already one nic doing the setup
					System.out.println("PATH:Router "+this.getAddress()+" received a path command from router: "+currentPacket.getSource());
					//send the wait messsage
					Packet wait = new Packet(this.getAddress(),currentPacket.getSource(),currentPacket.getDSCP());
					wait.setOAM(true);
					wait.setData("wait "+currentPacket.getDest());
					// send the wait information
					System.out.println("WAIT: Router "+this.getAddress()+" sent a wait command destined to "+wait.getDest());
					nic.sendPacket(wait, this);
				}
			}else if(currentPacket.getData().startsWith("wait")){
				System.out.println("WAIT: Router "+this.getAddress()+" received a WAIT command from "+currentPacket.getSource());
				//send the setup again
				Packet setup_ag = new Packet(this.getAddress(),this.getIntFromEndOfString(currentPacket.getData()),currentPacket.getDSCP());
				setup_ag.setOAM(true);
				setup_ag.setData("path");
				System.out.println("PATH: Router "+this.getAddress()+" sent a PATH command destined to "+setup_ag.getDest());
				nic.sendPacket(setup_ag, this);
			}else if(currentPacket.getData().startsWith("resv")){
				if(currentPacket.getData().equals("resvconf")){
					System.out.println("RESVCONF: Router "+this.getAddress()+" received a RESVCONF message from"+currentPacket.getSource());
				}else{
					if(currentPacket.getDest() != this.getAddress()){ // if the current packet hasn't arrived the dest
					    
						// receive the resv vc_number
						System.out.println("RESV: Router "+this.getAddress()+" received a resource reservation command from"+currentPacket.getSource());
						
						int input_vc =  this.vc; 
						this.vc++;					
				
						//put the nic_vc pair and input vc into the VCtoVC lookup table
						NICVCPair nic_vc_pair = new NICVCPair(nic,this.getIntFromEndOfString(currentPacket.getData()));
						//System.out.println(nic_vc_pair);
						VCtoVC.put(input_vc, nic_vc_pair);
					
						//pass the RESV packet
						System.out.println("RESV: Router "+this.getAddress()+" sent a RESV command destined to "+currentPacket.getDest());
						currentConnAttemptNIC.sendPacket(currentPacket, this);
						this.currentConnAttemptNIC = null;
					}else{// if the current packet has arrived the dest
						// the source of the path has received the RESV message	
						System.out.println("RESV: Router "+this.getAddress()+" received a resource reservation command from"+currentPacket.getSource());
					
						//store the label and the destination in the table
						destToLabelTable.put(currentPacket.getSource(),this.getIntFromEndOfString(currentPacket.getData()));
					
						//send the RESVConf message
						Packet resvconf= new Packet(this.getAddress(),currentPacket.getSource(),currentPacket.getDSCP());
						resvconf.setOAM(true);
						resvconf.setData("resvconf");
						System.out.println("RESVCONF: Router "+this.getAddress()+" sent a RESVCONF command to"+resvconf.getDest());
						nic.sendPacket(resvconf, this);
						this.currentConnAttemptNIC = null;
						
						//send all the packets in the tempBuffer, if its connection has been set up
						if(!tempBuffer.isEmpty()){
							for(int i=0;i<tempBuffer.size();i++){
								if(tempBuffer.get(i).getDest() == currentPacket.getSource()){//if the packet in tempBuffer has the same destination as this channel has
									//send the packet
									tempBuffer.get(i).addMPLSheader(new MPLS(destToLabelTable.get(tempBuffer.get(i).getDest()),0,1));
									nic.sendPacket(tempBuffer.get(i), this);//send the packet
									tempBuffer.remove(i);
									i--;
								}
							}
						}
					}
				}
			}
		}else{//if it is not OAM packets
		//MPLS label switching 
			
			MPLS mplsHeader = currentPacket.popMPLSheader();
			if(this.VCtoVC != null){
				if(this.VCtoVC.get(mplsHeader.getLabel()) != null){//hasn't arrived the destination
					LSRNIC outNic = this.VCtoVC.get(mplsHeader.getLabel()).getNIC();
					int outLabel = this.VCtoVC.get(mplsHeader.getLabel()).getVC();
					MPLS newMplsHeader = new MPLS(outLabel,mplsHeader.getTrafficClass(),mplsHeader.getStackingBit());
					currentPacket.addMPLSheader(newMplsHeader);
					outNic.sendPacket(currentPacket, this);
				}else{// has arrived the destination
					System.out.println("Packet from"+currentPacket.getSource()+" to "+currentPacket.getDest()+"has arrived!");
				}
			}
		}
	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP){
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
		
		if(!destToLabelTable.containsKey(destination)){//if the router hasn't established the channel to dest
			//setup the LSP
			Packet setupPack = new Packet(this.getAddress(),destination,DSCP);
			setupPack.setOAM(true);
			setupPack.setData("path");
			
			System.out.println("PATH:Router "+this.getAddress()+" sent a path command to Router "+destination);
			this.sendPacket(setupPack);
			
			
			//enter the packet into tempBuffer
			this.tempBuffer.add(newPacket);
		}else{
			//then send the packet
			this.sendPacket(newPacket);
		}
	}
	
	
	/**
	 * This method allocates bandwidth for a specific traffic class from the current router to the destination router
	 * @param dest destination router id
	 * @param PHB 0=EF, 1=AF, 2=BE
	 * @param Class AF classes 1,2,3,4. (0 if EF or BE)
	 * @param Bandwidth number of packets per time unit for this PHB/Class
	 * @since 1.0
	 */
	public void allocateBandwidth(int dest, int PHB, int Class, int Bandwidth){
		
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		//normal IP routing to test the routing algorithm
		if(newPacket.isOAM()){
		if(routingTable.containsKey(newPacket.getDest())){//if destination is reachable
				if(routingTable.get(newPacket.getDest()) == null){ //the packet is destined to itself 
					System.out.println("This packet is target to itself!");
				}else{
					System.out.println("packet destined to "+newPacket.getDest()+" has been sent");
					routingTable.get(newPacket.getDest()).sendPacket(newPacket, this);
				}
			}
		}else{
			//MPLS packet sending
			newPacket.addMPLSheader(new MPLS(destToLabelTable.get(newPacket.getDest()),newPacket.getDSCP(),1));
			this.nics.get(0).sendPacket(newPacket, this);
		}
	}

	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void recievePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).recievePackets();
	}

	public HashMap<Integer,LSRNIC> getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(HashMap<Integer,LSRNIC> routingTable) {
		this.routingTable = routingTable;
	}
	
	/*
	 *  return all the connected LSR as an arrayList ,this is used in the routing algorithm
	 */
	public ArrayList<LSR>getNeighbors(){
		ArrayList<LSR> neighbors = new ArrayList<LSR>();
		for(LSRNIC n: nics){
			if(n.getLink().getR1NIC().getParent() == this){
				neighbors.add(n.getLink().getR2NIC().getParent());
			}else{
				neighbors.add(n.getLink().getR1NIC().getParent());
			}
		}
		return neighbors;
	}
	
	public ArrayList<LSRNIC> getNics() {
		return nics;
	}

	public void setNics(ArrayList<LSRNIC> nics) {
		this.nics = nics;
	}
	/*
	 *  return the nic which is connected the lsr. if no connection return null 
	 */
	
	public LSRNIC getNicByLSR(LSR lsr){
		for(LSRNIC n:this.nics){
			if(n.getLink().getR1NIC().getParent() == lsr){
				return n;
			}
			if(n.getLink().getR2NIC().getParent() == lsr){
				return n;
			}
		}
		return null;
	}
	
	public void PrintRT(){
		System.out.println("routingTable size: "+routingTable.size());
		for(Integer k:routingTable.keySet()){
			System.out.println("addr:"+k+"NIC: " +routingTable.get(k));
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
				System.out.println("Could not get int from end of string");
			return -1;
		}
	}
}
