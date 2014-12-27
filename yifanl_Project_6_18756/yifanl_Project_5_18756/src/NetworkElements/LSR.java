package NetworkElements;

import java.util.*;

import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private HashMap<Integer,LSRNIC> routingTable = new HashMap<Integer,LSRNIC>();//IP routing table
	private HashMap<Integer,Integer> destToLabelTable = new HashMap<Integer,Integer>();  // a map of destination address to the path label
	private int nextLabel = 1;
	private HashMap<Integer, NICVCPair> PSCTable = new HashMap<Integer, NICVCPair>(); // a map of input NIC and input label to output NIC and output label
	private HashMap<String,NICStringPair> LSCTable = new HashMap<String,NICStringPair>();
	private ArrayList<Packet> tempBuffer=new ArrayList<Packet>(); //temporary buffer for path that hasn't been setup
	private int LSRType;//0 - PSC router, 1- LSC router, 2- hybrid router with PSC and LSC
	private int suggestLabel;//DownStreamSuggested label
	private int sourceAddr;
	private int destAddr;
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
	}
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address, boolean psc, boolean lsc ){
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
	 * data celll label switching is completing here.
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if(this.LSRType == 0){//receive code for PSC router
		  if (currentPacket.isOAM()){
			// What's OAM for?
			if(currentPacket.getOAMMsg().startsWith("path(psc)")){
				
				System.out.println("PATH:Router "+this.getAddress()+" received a path command from Router "+currentPacket.getSource()
						+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
				
				
			  if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
				//find the next hop
				LSR nexthop = this.getNeighborByNic(this.routingTable.get(currentPacket.getDest()));
				if(nexthop.getLSRType() == 1){//if the next hop is Lamda Switching router.
					//this just wont happen since type 0 router cannot be connected to type 1
				}else if(nexthop.getLSRType() == 2 || nexthop.getLSRType() == 0){// if the next hop is not LSC router
					//send the Path msg to the next hop directly
					//generate the suggesting label for PSC DownStream
					int DownStreamOutputPSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
					int DownStreamInputPSCLabel = this.nextLabel;
					this.nextLabel++;
					this.PSCTable.put(DownStreamInputPSCLabel, new NICVCPair(
							this.routingTable.get(currentPacket.getSource()),DownStreamOutputPSCLabel));
					System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+DownStreamInputPSCLabel
							+",Output:"+DownStreamOutputPSCLabel);
					//generate the PSC PATH message
					Packet path = new Packet(currentPacket.getSource(),currentPacket.getDest());
					path.setOAM(true,"path(psc) "+DownStreamInputPSCLabel);
					
					//send the PSC PATH message to the next hop
					this.sendPacket(path);
					System.out.println("PATH(PSC):Router "+this.getAddress()+" sent a PATH command to Router "+path.getDest()
							+" with suggesting Label: "+DownStreamInputPSCLabel);
				}
			  }else{// if the packet arrived the destination
				  //store the Downstream label
				  int DownStreamOutputPSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
				  this.PSCTable.put(currentPacket.getSource()+1000, new NICVCPair(
							this.routingTable.get(currentPacket.getSource()),DownStreamOutputPSCLabel));
				  System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+(currentPacket.getSource()+1000)
							+",Output:"+DownStreamOutputPSCLabel);
				  //create the Upstream label 
				  int UpStreamInputPSCLabel = this.nextLabel;
				  this.nextLabel++;
				  this.PSCTable.put(UpStreamInputPSCLabel, null);
				  System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+UpStreamInputPSCLabel
							+",Output:"+null);
				  
				  //store the new path in the table
				  this.destToLabelTable.put(currentPacket.getSource(), currentPacket.getSource()+1000);
				  
				  //Generate the Resv message
				  Packet resv = new Packet(this.getAddress(),currentPacket.getSource());
				  resv.setOAM(true,"resv(psc) "+UpStreamInputPSCLabel);
				  this.sendPacket(resv);
				
				  System.out.println("RESV(PSC):Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
						+" with suggesting Label: "+UpStreamInputPSCLabel);

			  }
			}else if(currentPacket.getOAMMsg().startsWith("resv(psc)")){
				System.out.println("RESV(PSC):Router "+this.getAddress()+" received a RESV command from Router "+currentPacket.getSource()
						+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
				if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
					//find the PSC label that hasn't been used.
					int UpStreamInputLSCLabel=this.nextLabel;
					this.nextLabel++;
					int UpStreamOutputLSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
					//insert the Upstream label into the PSCTable
					this.PSCTable.put(UpStreamInputLSCLabel, new NICVCPair(
								this.routingTable.get(currentPacket.getDest()),UpStreamOutputLSCLabel));
					
					System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+UpStreamInputLSCLabel
							+",Output:"+UpStreamOutputLSCLabel);
					//send a new RESV message to next PSC router to setup the path
					Packet resv = new Packet(currentPacket.getSource(),currentPacket.getDest());
					resv.setOAM(true, "resv(psc) "+UpStreamInputLSCLabel);
					this.sendPacket(resv);
					System.out.println("RESV(PSC):Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
							+" with suggesting Label: "+UpStreamInputLSCLabel);
					
				}else{// resv arrive the dest
					//find the PSC label that hasn't been used.
					
					this.nextLabel++;
					int UpStreamOutputLSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
					//insert the Upstream label into the PSCTable
					this.PSCTable.put(currentPacket.getSource()+1000, new NICVCPair(
								this.routingTable.get(currentPacket.getSource()),UpStreamOutputLSCLabel));
					
					System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+(currentPacket.getSource()+1000)
							+",Output:"+UpStreamOutputLSCLabel);
					
					//send the RESVCONF
					Packet resvConf = new Packet(currentPacket.getDest(),currentPacket.getSource());
					resvConf.setOAM(true,"resvconf(psc)");
					
					this.sendPacket(resvConf);
					System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+resvConf.getDest());
					
					//Print out the path has been setup
					System.out.println("Path from "+currentPacket.getDest()+" to "+currentPacket.getSource()+" has been setup!");
					
					//store the new path in the table
					this.destToLabelTable.put(currentPacket.getSource(), currentPacket.getSource()+1000);
					
					//send the packets in the temp buffer away.
					for(Packet p:tempBuffer){
						this.sendPacket(p);
					}
				}

			}else if(currentPacket.getOAMMsg().startsWith("resvconf(psc)")){
				System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" received a RESVCONF command from Router "+currentPacket.getSource());
				if(currentPacket.getDest() != this.address){
					this.sendPacket(currentPacket);
					System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+currentPacket.getDest());
				}
			}
		  }else{//if the packet is data packet
				//MPLS label switching 
				MPLS mplsHeader = currentPacket.popMPLSheader();
				if(this.PSCTable != null){
					if(this.PSCTable.get(mplsHeader.getLabel()) != null){//hasn't arrived the destination
						LSRNIC outNic = this.PSCTable.get(mplsHeader.getLabel()).getNIC();
						int outLabel = this.PSCTable.get(mplsHeader.getLabel()).getVC();
						MPLS newMplsHeader = new MPLS(outLabel,mplsHeader.getTrafficClass(),mplsHeader.getStackingBit());
						currentPacket.addMPLSheader(newMplsHeader);
						outNic.sendPacket(currentPacket, this);
					}else{// has arrived the destination
						System.out.println("Packet from"+currentPacket.getSource()+" to "+currentPacket.getDest()+"has arrived!");
					}
				}				
		  }
		}else if(this.LSRType == 1){
			if (currentPacket.isOAM()){
				// What's OAM for?
				if(currentPacket.getOAMMsg().startsWith("path(lsc)")){
					System.out.println("PATH(LSC):Router "+this.getAddress()+" received a path command from Router "+currentPacket.getSource()
							+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
					  if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
							//find the LSC label that hasn't been used.
							String DownStreamInputLSCLabel=this.suggestLSCLabel();
							String DownStreamOutputLSCLabel = this.getEndOfString(currentPacket.getOAMMsg());
							//insert the downstream label into the LSCTable
							if(DownStreamInputLSCLabel!=""){
								this.LSCTable.put(DownStreamInputLSCLabel, new NICStringPair(
										this.getOpNic(this.routingTable.get(currentPacket.getSource())),DownStreamOutputLSCLabel));
				
							}
							//send a new PATH message to next PSC router to setup the path
							Packet path = new Packet(currentPacket.getSource(),currentPacket.getDest());
							path.setOAM(true, "path(lsc) "+DownStreamInputLSCLabel);
							this.sendPacket(path);
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+DownStreamInputLSCLabel
									+",Output:"+DownStreamOutputLSCLabel+"/"+this.getOpNic(this.routingTable.get(
											currentPacket.getSource())).getLink().getOtherNIC(this.getOpNic(this.routingTable.get(currentPacket.getSource()))).getParent().getAddress());
							
							System.out.println("PATH(LSC):Router "+this.getAddress()+" sent a path command to Router "+path.getDest()
									+" with suggesting Label: "+DownStreamInputLSCLabel);

					  }
				}else if(currentPacket.getOAMMsg().startsWith("resv(lsc)")){
					System.out.println("RESV(LSC):Router "+this.getAddress()+" received a RESV command from Router "+currentPacket.getSource()
							+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
					if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
						//find the LSC label that hasn't been used.
						String UpStreamInputLSCLabel=this.suggestLSCLabel();
						String UpStreamOutputLSCLabel = this.getEndOfString(currentPacket.getOAMMsg());
						//insert the Upstream label into the LSCTable
						if(UpStreamInputLSCLabel!=""){
							this.LSCTable.put(UpStreamInputLSCLabel, new NICStringPair(
									this.getOpNic(this.routingTable.get(currentPacket.getSource())),UpStreamOutputLSCLabel));
						}
						
						int upnexthopAddr = this.getNeighborByNic(this.getOpNic(this.routingTable.get(currentPacket.getSource()))).getAddress();
						
						System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+UpStreamInputLSCLabel
								+",Output:"+UpStreamOutputLSCLabel+"/"+upnexthopAddr);
						//send a new RESV message to next LSC router to setup the path
						Packet resv = new Packet(currentPacket.getSource(),currentPacket.getDest());
						resv.setOAM(true, "resv(lsc) "+UpStreamInputLSCLabel);
						this.sendPacket(resv);
						System.out.println("RESV(LSC):Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
								+" with suggesting Label: "+UpStreamInputLSCLabel);
						
					}
				}else if(currentPacket.getOAMMsg().startsWith("path(psc)")){
					System.out.println("PATH(PSC):Router "+this.getAddress()+" received a path command from Router "+currentPacket.getSource()
							+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
					this.sendPacket(currentPacket);
					System.out.println("PATH(RSV):Router "+this.getAddress()+" sent a path command to Router "+currentPacket.getDest()
							+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
				}else if(currentPacket.getOAMMsg().startsWith("resv(psc)")){
					System.out.println("RESV(PSC):Router "+this.getAddress()+" received a RESV command from Router "+currentPacket.getSource()
							+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
					this.sendPacket(currentPacket);
					System.out.println("RESV(RSV):Router "+this.getAddress()+" sent a RESV command to Router "+currentPacket.getDest()
							+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
				}else if(currentPacket.getOAMMsg().startsWith("resvconf(psc)")){
					System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" received a RESVCONF command from Router "+currentPacket.getSource());
					if(currentPacket.getDest() != this.address){
						this.sendPacket(currentPacket);
						System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+currentPacket.getDest());
					}
				}else if(currentPacket.getOAMMsg().startsWith("resvconf(lsc)")){
					System.out.println("RESVCONF(LSC):Router "+this.getAddress()+" received a RESVCONF command from Router "+currentPacket.getSource());
					if(currentPacket.getDest() != this.address){
						this.sendPacket(currentPacket);
						System.out.println("RESVCONF(LSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+currentPacket.getDest());
					}
				}
			}else{//data cell
				//System.out.println("Router "+this.getAddress()+" data from "+currentPacket.getSource()+" to "+currentPacket.getDest());
				if(this.LSCTable != null){
					
					//set up the LSC label
					currentPacket.setOpticalLabel(this.LSCTable.get(currentPacket.getOpticalLabel()).getLabel());
					LSRNIC outNic = this.LSCTable.get(currentPacket.getOpticalLabel()).getNic();
					outNic.sendPacket(currentPacket, this);
				}
			}
		}else if(this.LSRType == 2){
			 if (currentPacket.isOAM()){
					// What's OAM for?
					if(currentPacket.getOAMMsg().startsWith("path(psc)")){
						
						System.out.println("PATH(PSC):Router "+this.getAddress()+" received a path command from Router "+currentPacket.getSource()
								+" with suggesting Label: "+this.getIntFromEndOfString(currentPacket.getOAMMsg()));
						
						//find the next hop
						LSR nexthop = this.getNeighborByNic(this.routingTable.get(currentPacket.getDest()));
					  if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
						if(nexthop.getLSRType() == 1){//if the next hop is Lamda Switching router.
							this.sourceAddr = currentPacket.getSource();
							this.destAddr = currentPacket.getDest();
							//store the suggesting label from the previous PSC router
							this.suggestLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
							//find out the next PSC router 
							LSR nextPSCRouter = this.getNextPSChop(currentPacket.getDest());
							
							//find the LSC label that hasn't been used.
							String DownStreamInputLSCLabel=this.suggestLSCLabel();
						
							//insert the downstream label into the LSCTable
							if(DownStreamInputLSCLabel!=""){
								this.LSCTable.put(DownStreamInputLSCLabel, null);
							}
							//send a new PATH message to next LSC router to setup the path
							Packet path = new Packet(this.getAddress(),nextPSCRouter.getAddress());
							path.setOAM(true, "path(lsc) "+DownStreamInputLSCLabel);
							this.sendPacket(path);
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+DownStreamInputLSCLabel
									+",Output:");
							
							System.out.println("PATH(LSC):Router "+this.getAddress()+" sent a path command to Router "+path.getDest()
									+" with suggesting Label: "+DownStreamInputLSCLabel);
							//if the path between two PSC routers has already been set up, send the PATH msg to destination through the path
							
						}else if(nexthop.getLSRType() == 2 || nexthop.getLSRType() == 0){// if the next hop is not LSC router
							//send the Path msg to the next hop directly
							//generate the suggesting label for PSC DownStream
							int DownStreamOutputPSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
							int DownStreamInputPSCLabel = this.nextLabel;
							this.nextLabel++;
							this.PSCTable.put(DownStreamInputPSCLabel, new NICVCPair(
									this.routingTable.get(currentPacket.getSource()),DownStreamOutputPSCLabel));
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+DownStreamInputPSCLabel
									+",Output:"+DownStreamOutputPSCLabel);
							//generate the PSC PATH message
							Packet path = new Packet(currentPacket.getSource(),currentPacket.getDest());
							path.setOAM(true,"path(psc) "+DownStreamInputPSCLabel);
							
							//send the PSC PATH message to the next hop
							this.sendPacket(path);
							System.out.println("PATH(PSC):Router "+this.getAddress()+" sent a PATH command to Router "+path.getDest()
									+" with suggesting Label: "+DownStreamInputPSCLabel);
						}
					  }else{// if the packet arrived the destination
						  //store the Downstream label
						  int DownStreamOutputPSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
						  this.PSCTable.put(currentPacket.getSource()+1000, new NICVCPair(
									this.routingTable.get(currentPacket.getSource()),DownStreamOutputPSCLabel));
						  System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+(currentPacket.getSource()+1000)
									+",Output:"+DownStreamOutputPSCLabel);
						  //create the Upstream label 
						  int UpStreamInputPSCLabel = this.nextLabel;
						  this.nextLabel++;
						  this.PSCTable.put(UpStreamInputPSCLabel, null);
						  System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+UpStreamInputPSCLabel
									+",Output:"+null);
						  
						  //store the new path in the table
						  this.destToLabelTable.put(currentPacket.getSource(), currentPacket.getSource()+1000);						  
						  
						  //Generate the Resv message
						  Packet resv = new Packet(this.getAddress(),currentPacket.getSource());
						  resv.setOAM(true,"resv(psc) "+UpStreamInputPSCLabel);
						  this.sendPacket(resv);
						
						  System.out.println("RESV:Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
								+" with suggesting Label: "+UpStreamInputPSCLabel);

					  }
					}else if (currentPacket.getOAMMsg().startsWith("path(lsc)")){
						System.out.println("PATH(LSC):Router "+this.getAddress()+" received a path command from Router "+currentPacket.getSource()
								+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
			
						 if(currentPacket.getDest() == this.getAddress()){//if the packet has arrived the destination
							 
							 //set lSC label to be Source
							String DownStreamInputLSCLabel=String.valueOf(currentPacket.getSource());
							String DownStreamOutputLSCLabel = this.getEndOfString(currentPacket.getOAMMsg());
							//insert the downstream label into the LSCTable
							if(DownStreamInputLSCLabel!=""){
								this.LSCTable.put(DownStreamInputLSCLabel, new NICStringPair(
										this.getOpNic(this.routingTable.get(currentPacket.getSource())),DownStreamOutputLSCLabel));
							}
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+DownStreamInputLSCLabel
								+",Output:"+DownStreamOutputLSCLabel);

							//find the LSC label that hasn't been used.
							String UpStreamInputLSCLabel=this.suggestLSCLabel();
							//insert the upstream label into the LSCTable
							if(DownStreamInputLSCLabel!=""){
								this.LSCTable.put(UpStreamInputLSCLabel, null);
							}
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+UpStreamInputLSCLabel
								+",Output:");

							//Generate the Resv message
							Packet resv = new Packet(this.getAddress(),currentPacket.getSource());
							resv.setOAM(true,"resv(lsc) "+UpStreamInputLSCLabel);
							this.sendPacket(resv);
						
							System.out.println("RESV(LSC):Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
								+" with suggesting Label: "+UpStreamInputLSCLabel);

						 }
					}else if(currentPacket.getOAMMsg().startsWith("resv(lsc)")){
						System.out.println("RESV(LSC):Router "+this.getAddress()+" received a RESV command from Router "+currentPacket.getSource()
								+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
						if(currentPacket.getDest()== this.getAddress()){//if the packet has arrived the destination
							String UpStreamInputLSCLabel=String.valueOf(currentPacket.getSource());
							String UpStreamOutputLSCLabel = this.getEndOfString(currentPacket.getOAMMsg());
							//insert the Upstream label into the LSCTable
							this.LSCTable.put(UpStreamInputLSCLabel, new NICStringPair(
										this.getOpNic(this.routingTable.get(currentPacket.getSource())),UpStreamOutputLSCLabel));
						
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:LSC/"+UpStreamInputLSCLabel
									+",Output:"+UpStreamOutputLSCLabel);
							
							//generate the suggesting label for PSC DownStream
							int DownStreamInputPSCLabel = this.nextLabel;
							this.nextLabel++;
							this.PSCTable.put(DownStreamInputPSCLabel, new NICVCPair(
									this.routingTable.get(this.sourceAddr),this.suggestLabel));
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+DownStreamInputPSCLabel
									+",Output:"+this.suggestLabel);
							
							//generate the PSC PATH message
							Packet path = new Packet(this.sourceAddr,this.destAddr);
							path.setOAM(true,"path(psc) "+DownStreamInputPSCLabel);
							
							//send the PSC PATH message to the next hop
							this.sendPacket(path);
							System.out.println("PATH(PSC):Router "+this.getAddress()+" sent a PATH command to Router "+path.getDest()
									+" with suggesting Label: "+DownStreamInputPSCLabel);
							
							//send the RESVCONF
							Packet resvConf = new Packet(currentPacket.getDest(),currentPacket.getSource());
							resvConf.setOAM(true,"resvconf(lsc)");
							
							this.sendPacket(resvConf);
							System.out.println("RESVCONF(LSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+resvConf.getDest());

						}
					}else if(currentPacket.getOAMMsg().startsWith("resv(psc)")){
						System.out.println("RESV(PSC):Router "+this.getAddress()+" received a RESV command from Router "+currentPacket.getSource()
								+" with suggesting Label: "+this.getEndOfString(currentPacket.getOAMMsg()));
						if(currentPacket.getDest() != this.getAddress()){//if the packet hasn't arrived the destination
							//find the PSC label that hasn't been used.
							int UpStreamInputLSCLabel=this.nextLabel;
							this.nextLabel++;
							int UpStreamOutputLSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
							//insert the Upstream label into the PSCTable
							this.PSCTable.put(UpStreamInputLSCLabel, new NICVCPair(
										this.routingTable.get(currentPacket.getSource()),UpStreamOutputLSCLabel));
							
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+UpStreamInputLSCLabel
									+",Output:"+UpStreamOutputLSCLabel);
							//send a new RESV message to next PSC router to setup the path
							Packet resv = new Packet(currentPacket.getSource(),currentPacket.getDest());
							resv.setOAM(true, "resv(psc) "+UpStreamInputLSCLabel);
							this.sendPacket(resv);
							System.out.println("RESV(PSC):Router "+this.getAddress()+" sent a RESV command to Router "+resv.getDest()
									+" with suggesting Label: "+UpStreamInputLSCLabel);
							
						}else{// resv arrive the dest
							//find the PSC label that hasn't been used.
							
							this.nextLabel++;
							int UpStreamOutputLSCLabel = this.getIntFromEndOfString(currentPacket.getOAMMsg());
							//insert the Upstream label into the PSCTable
							this.PSCTable.put(currentPacket.getSource()+1000, new NICVCPair(
										this.routingTable.get(currentPacket.getSource()),UpStreamOutputLSCLabel));
							
							System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+(currentPacket.getSource()+1000)
									+",Output:"+UpStreamOutputLSCLabel);
							
							//send the RESVCONF
							Packet resvConf = new Packet(currentPacket.getDest(),currentPacket.getSource());
							resvConf.setOAM(true,"resvconf(psc)");
							
							this.sendPacket(resvConf);
							System.out.println("RESVCONF:Router "+this.getAddress()+" sent a RESVCONF command to Router "+resvConf.getDest());							
							
							//Print out the path has been setup
							System.out.println("Path from "+currentPacket.getDest()+" to "+currentPacket.getSource()+" has been setup!");
							
							//store the new path in the table
							this.destToLabelTable.put(currentPacket.getSource(), currentPacket.getSource()+1000);
							
							//send the packets in the temp buffer away.
							for(Packet p:tempBuffer){
								this.sendPacket(p);
							}
						}

					}else if(currentPacket.getOAMMsg().startsWith("resvconf(psc)")){
						System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" received a RESVCONF command from Router "+currentPacket.getSource());
						if(currentPacket.getDest() != this.address){
							this.sendPacket(currentPacket);
							System.out.println("RESVCONF(PSC):Router "+this.getAddress()+" sent a RESVCONF command to Router "+currentPacket.getDest());
						}
					}else if(currentPacket.getOAMMsg().startsWith("resvconf(lsc)")){
						System.out.println("RESVCONF(LSC):Router "+this.getAddress()+" received a RESVCONF command from Router "+currentPacket.getSource());
					}
				  }else{//if the packet is data packet
						//MPLS label switching 
						MPLS mplsHeader = currentPacket.popMPLSheader();
						if(this.PSCTable != null){
							if(this.PSCTable.get(mplsHeader.getLabel()) != null){//hasn't arrived the destination
								if(this.LSCTable != null){
									if(currentPacket.getOpticalLabel().equals("")){//the packet hasn't arrived
										int outLabel = this.PSCTable.get(mplsHeader.getLabel()).getVC();
										MPLS newMplsHeader = new MPLS(outLabel,mplsHeader.getTrafficClass(),mplsHeader.getStackingBit());
										currentPacket.addMPLSheader(newMplsHeader);
										
										LSR nextPSCRouter = this.getNextPSChop(currentPacket.getDest());
										//set up the LSC label
										currentPacket.setOpticalLabel(this.LSCTable.get(String.valueOf(nextPSCRouter.getAddress())).getLabel());
										LSRNIC outNic = this.LSCTable.get(String.valueOf(nextPSCRouter.getAddress())).getNic();
										outNic.sendPacket(currentPacket, this);
									}else{// the packet has arrived
										// double check this assumption
										if(this.LSCTable.get(currentPacket.getOpticalLabel()) == null){
											LSRNIC outNic = this.PSCTable.get(mplsHeader.getLabel()).getNIC();
											//System.out.println("label:"+mplsHeader.getLabel()+"  "+this.getNeighborByNic(outNic).getAddress());
											int outLabel = this.PSCTable.get(mplsHeader.getLabel()).getVC();
											MPLS newMplsHeader = new MPLS(outLabel,mplsHeader.getTrafficClass(),mplsHeader.getStackingBit());
											currentPacket.addMPLSheader(newMplsHeader);
											outNic.sendPacket(currentPacket, this);
										}
									}
								}else{// no LSC = send the packet by PSC
									LSRNIC outNic = this.PSCTable.get(mplsHeader.getLabel()).getNIC();
									int outLabel = this.PSCTable.get(mplsHeader.getLabel()).getVC();
									MPLS newMplsHeader = new MPLS(outLabel,mplsHeader.getTrafficClass(),mplsHeader.getStackingBit());
									currentPacket.addMPLSheader(newMplsHeader);
									outNic.sendPacket(currentPacket, this);
								}
							}else{// has arrived the destination
								System.out.println("Packet from"+currentPacket.getSource()+" to "+currentPacket.getDest()+"has arrived!");
							}
						}
						
				  }
		}
	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @since 1.0
	 */
	public void createPacket(int destination){
		Packet newPacket= new Packet(this.getAddress(), destination);
		
		if(!destToLabelTable.containsKey(destination)){//if the router hasn't established the channel to dest
			//setup the LSP
			Packet setupPack = new Packet(this.getAddress(),destination);
			
			setupPack.setOAM(true,"path(psc) "+nextLabel);
			
			//find out the upstream input nic
			LSRNIC UpstreamOutputNic = this.routingTable.get(destination);
			//insert the NIC label pair into the table
			this.PSCTable.put(this.nextLabel, null);
			System.out.println("Router "+this.getAddress()+", Route ADD,"+" Input:PSC/"+nextLabel+",Output:null");
			
			System.out.println("PATH(PSC):Router "+this.getAddress()+" sent a path command to Router "+destination
					+" with suggesting Label: "+nextLabel);
			nextLabel++;//increment the suggesting label
			
			this.sendPacket(setupPack);
			
			//enter the packet into tempBuffer
			this.tempBuffer.add(newPacket);
		}else{
			//if not send the packet
			this.sendPacket(newPacket);
		}
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC (and wavelength if LSC router).
		if(newPacket.isOAM()){
			if(routingTable.containsKey(newPacket.getDest())){//if destination is reachable
				if(routingTable.get(newPacket.getDest()) == null){ //the packet is destined to itself 
					System.out.println("This packet is target to itself!");
				}else{
					routingTable.get(newPacket.getDest()).sendPacket(newPacket, this);
				}
			}
		}else{
			//MPLS packet sending
			
			int label = this.PSCTable.get(destToLabelTable.get(newPacket.getDest())).getVC();
			newPacket.addMPLSheader(new MPLS(label,0,1));
			this.PSCTable.get(destToLabelTable.get(newPacket.getDest())).getNIC().sendPacket(newPacket, this);
		}
		
	}

	/**
	 * This method should send the keep alive packets for routes for each the router is an inbound router
	 * @since 1.0
	 */
	public void sendKeepAlivePackets() {
		
		//This method should send the keep alive packets for routes for each the router is an inbound router
		
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		sendKeepAlivePackets();
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).receivePackets();
	}
	
	public void sendKeepAlive(int dest, String label){
			Packet p = new Packet(this.getAddress(), dest, label);
			p.setOAM(true, "KeepAlive");
			this.sendPacket(p);
	}

	public HashMap<Integer,LSRNIC> getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(HashMap<Integer,LSRNIC> routingTable) {
		this.routingTable = routingTable;
	}
	
	public LSR getNeighborByNic(LSRNIC nic){
		return nic.getLink().getOtherNIC(nic).getParent();
	}
	
	/*
	 *  return all the connected LSR as an arrayList ,this is used in the routing algorithm
	 */
	public ArrayList<LSR> getNeighbors(){
		ArrayList<LSR> neighbors = new ArrayList<LSR>();
		for(LSRNIC n: nics){
			if(n.getLink().getOptical() == false){
				if(n.getLink().getR1NIC().getParent() == this){
					neighbors.add(n.getLink().getR2NIC().getParent());
				}else{
					neighbors.add(n.getLink().getR1NIC().getParent());
				}
			}
		}
		return neighbors;
	}
	
	/*
	 *  return the IP nic which is connected the lsr. if no connection return null 
	 */
	
	public LSRNIC getNicByLSR(LSR lsr){
		for(LSRNIC n:this.nics){
			if(n.getLink().getOptical() == false){
				if(n.getLink().getR1NIC().getParent() == lsr){
					return n;
				}
				if(n.getLink().getR2NIC().getParent() == lsr){
					return n;
				}
			}
		}
		return null;
	}
	
	// print out the routing table
	public void PrintRT(){
		System.out.println("routingTable size: "+routingTable.size());
		for(Integer k:routingTable.keySet()){
			if(routingTable.get(k) == null){
			System.out.println("addr:"+k+"NIC: " +routingTable.get(k));
			}else{
				System.out.println("addr:"+k+"NIC: " +routingTable.get(k).getLink().getOptical());
			}
			
		}
		
	}

	public int getLSRType() {
		return LSRType;
	}

	public void setLSRType(int lSRType) {
		LSRType = lSRType;
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
	
	//get the 2nd part  of a string
	private String getEndOfString(String string){
		// Try getting the number from the end of the string
		try{
			String num = string.split(" ")[string.split(" ").length-1];
			return num;
		}
		// Couldn't do it, so return -1
		catch(Exception e){
				System.out.println("Could not get int from end of string");
			return "";
		}
	}
	
	//find the next PSC router during the path
	public LSR getNextPSChop(int destination){
		LSRNIC nexthop = this.routingTable.get(destination);
		LSR nextLSR = nexthop.getLink().getOtherNIC(nexthop).getParent();
		while(nextLSR.getLSRType() == 1){
			nexthop = nextLSR.routingTable.get(destination);
			nextLSR = nexthop.getLink().getOtherNIC(nexthop).getParent();
		}
		return nextLSR;
	}
	// suggest a LSC label that hasn't been used
	public String suggestLSCLabel(){
		boolean red =false;
		boolean green =false;
		boolean blue =false;
		boolean yellow =false;
		for(String k:this.LSCTable.keySet()){
			if(k.equals("red") || this.LSCTable.get(k).getLabel().equals("red")){
				red = true;
			}
			if(k.equals("green")|| this.LSCTable.get(k).getLabel().equals("green")){
				green = true;
			}
			if(k.equals("blue")|| this.LSCTable.get(k).getLabel().equals("blue")){
				blue = true;
			}
			if(k.equals("yellow")|| this.LSCTable.get(k).getLabel().equals("yellow")){
				yellow = true;
			}
			
		}
		
		if(!red){
			return "red";
		}
		if(!green){
			return "green";
		}
		if(!blue){
			return "blue";
		}
		if(!yellow){
			return "yellow";
		}
		return ""; // all the labels are occupied
	}
	
	//get the optical link going to the same LSR
	public LSRNIC getOpNic(LSRNIC nic){
		for(LSRNIC n:nics){
			if(n.getLink().getOptical() == true){
				if(n.getLink().getOtherNIC(n).getParent() == nic.getLink().getOtherNIC(nic).getParent()){
					return n;
				}
			}
		}
		return null;
	}
}
