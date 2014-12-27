package NetworkElements;

import java.util.*;
import java.net.*;
import DataTypes.*;

public class IPRouter implements IPConsumer{
	private ArrayList<IPNIC> nics = new ArrayList<IPNIC>();
	private HashMap<Inet4Address, IPNIC> forwardingTable = new HashMap<Inet4Address, IPNIC>();
	private int time = 0;
	private Boolean fifo=true, rr=false, wrr=false, wfq=false, routeEntirePacket=true;
	private HashMap<IPNIC, FIFOQueue> inputQueues = new HashMap<IPNIC, FIFOQueue>();
	private int lastNicServiced=-1, weightFulfilled=1;
	// remembering the queue rather than the interface number is useful for wfq
	private FIFOQueue lastServicedQueue = null;
	private double virtualTime = 0.0;
	
	//counter for weight
	int counter4weight = 0;
	
	//flag for switch the queue
	boolean flag_switch =false; 
	
	// FIFO queue declaring
	private FIFOQueue fifo_queue=null;
	/**
	 * The default constructor of a router
	 */
	public IPRouter(){
		
	}
	
	/**
	 * adds a forwarding address in the forwarding table
	 * @param destAddress the address of the destination
	 * @param nic the nic the packet should be sent on if the destination address matches
	 */
	public void addForwardingAddress(Inet4Address destAddress, IPNIC nic){
		forwardingTable.put(destAddress, nic);
	}
	
	/**
	 * receives a packet from the NIC
	 * @param packet the packet received
	 * @param nic the nic the packet was received on
	 */
	public void receivePacket(IPPacket packet, IPNIC nic){
		//enter the fifo_queue firstly every time a packet comes.
		if(fifo) fifo_queue.offer(packet);
		
		// enter the rr queue by its nic number
		if(rr){
			inputQueues.get(nic).offer(packet);
		}
		// enter the wrr queue by its nic number
		if(wrr){
			inputQueues.get(nic).offer(packet);
		}
		// If wfq set the expected finish time
		if(this.wfq){
			//caculate & output the est finish time here
			inputQueues.get(nic).offer(packet);	
			double expected_finish_time=0.0;
			//FIFOQueue temp_queue=inputQueues.get(nic);
			//System.out.println("lastsentpacket"+temp_queue.getLastSentPacket().getFinishTime());
			if(inputQueues.get(nic).getSizeofQueue() == 1 && inputQueues.get(nic).getLastSentPacket() != null){
				expected_finish_time = Math.max(inputQueues.get(nic).getLastSentPacket().getFinishTime(),virtualTime)+(double)(packet.getSize())/(double)(inputQueues.get(nic).getWeight());
				packet.setFinishTime(expected_finish_time);
			}else{
				expected_finish_time = Math.max((inputQueues.get(nic).secondLastPeek()==null)?0.0:inputQueues.get(nic).secondLastPeek().getFinishTime(),virtualTime)+(double)(packet.getSize())/(double)(inputQueues.get(nic).getWeight());
				packet.setFinishTime(expected_finish_time);
			}
		//	System.out.println("R(t):"+virtualTime);
			System.out.println("est finish time:"+expected_finish_time);
		}
	}
	
	public void forwardPacket(IPPacket packet){
		forwardingTable.get(packet.getDest()).sendIPPacket(packet);
	}
	
	public void routeBit(){
		/*
		 *  FIFO scheduler
		 */
		if(this.fifo) this.fifo();
			
		
		/*
		 *  RR scheduler
		 */
		if(this.rr) this.rr();
			
		
		/*
		 *  WRR scheduler
		 */
		if(this.wrr) this.wrr();
			
		
		/*
		 * WFQ scheduler
		 */
		if(this.wfq) this.wfq();
	}
	
	/**
	 * Perform FIFO scheduler on the queue
	 */
	private void fifo(){
		
		if(fifo_queue.peek() != null){// if there is still a packet in the queue
			
			//route the bits inside queue & increment the counter 
			this.fifo_queue.routeBit();		
			//delay all the bits inside the queue
			this.fifo_queue.tock();		

			//if all the bits in the first packet have entered the output queue.
			if(fifo_queue.peek().getSize() == fifo_queue.getBitsRoutedSinceLastPacketSent()){
				this.forwardPacket(fifo_queue.remove());
			}
		}
	}
	
	/**
	 * Perform round robin on the queue
	 */
	private void rr(){
		//find the number of queues which is not empty
		int number_of_working_queue=0;
		for(IPNIC ipnic: this.inputQueues.keySet()){
			if(this.inputQueues.get(ipnic).peek() != null){
				number_of_working_queue += 1;
			}
		}
		if(number_of_working_queue != 0){ //if there is a queue working
			if(!this.routeEntirePacket){ // RR bit by bit
		
				//find the right nic to be serviced.
				int nic_to_service = (lastNicServiced+1)%(number_of_working_queue);
				FIFOQueue queue_to_service = this.inputQueues.get(nics.get(nic_to_service));
				if(queue_to_service.peek() != null){// if there is still a packet in the queue
					//route the bit
					queue_to_service.routeBit();
					// send the packet if all the bits inside the packet have been routed.
					if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
						this.forwardPacket(queue_to_service.remove());
					}
				}		
				//update the last serviced Nic  
				lastNicServiced =nic_to_service; 
			}else{   // RR packet by packet
				//find the right queue to be serviced
				
				if(lastNicServiced != -1 && this.inputQueues.get(nics.get(lastNicServiced)).getBitsRoutedSinceLastPacketSent() != 0 ){//if the packet from the last serviced queue hasn't sent
					FIFOQueue queue_to_service = this.inputQueues.get(nics.get(lastNicServiced));
					//route the bit
					queue_to_service.routeBit();
					// send the packet if all the bits inside the packet have been routed.
					if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
						this.forwardPacket(queue_to_service.remove());
					}
				}else{//if the packet has been sent from the last serviced queue, process the next queue.
					//find the right nic to be serviced.
					int nic_to_service = (lastNicServiced+1)%(number_of_working_queue);
					FIFOQueue queue_to_service = this.inputQueues.get(nics.get(nic_to_service));
					if(queue_to_service.peek() != null){// if there is still a packet in the queue
						//route the bit
						queue_to_service.routeBit();
						// send the packet if all the bits inside the packet have been routed.
						if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
							this.forwardPacket(queue_to_service.remove());
						}
					}
					//update the last serviced Nic  
					lastNicServiced =nic_to_service; 
				}
				
				
			}
		}
	}
	
	/**
	 * Perform weighted round robin on the queue
	 */
	private void wrr(){
		//declaration 
		FIFOQueue queue_to_service=null;
		//find the number of queues which is not empty
		int number_of_working_queue=0;
		for(IPNIC ipnic: this.inputQueues.keySet()){
			if(this.inputQueues.get(ipnic).peek() != null){
				number_of_working_queue += 1;
			}
		}
		if(number_of_working_queue != 0){ //if there is a queue working
			if(!this.routeEntirePacket){ // WRR bit by bit
				//find the right queue to serve
				if(lastNicServiced == -1){ // treat the first round seperately
					queue_to_service = inputQueues.get(nics.get(0));
					if(queue_to_service.peek() != null){
						queue_to_service.routeBit();
						// send the packet if all the bits inside the packet have been routed.
						if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
							this.forwardPacket(queue_to_service.remove());
						}
						counter4weight = (counter4weight+1)%queue_to_service.getWeight();
					}
					lastNicServiced = 0;
				}else{// if it is not the first round
					if(counter4weight != 0){//the previous queue hasn't finished 
						queue_to_service = inputQueues.get(nics.get(lastNicServiced));
						if(queue_to_service.peek() != null){
							queue_to_service.routeBit();
							// send the packet if all the bits inside the packet have been routed.
							if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
								this.forwardPacket(queue_to_service.remove());
							}
							counter4weight = (counter4weight+1)%queue_to_service.getWeight();
						}else{//if the queue is already null
							counter4weight = 0;
						}
					}else{
						int nic_to_service = (lastNicServiced+1)%(number_of_working_queue);
						queue_to_service = this.inputQueues.get(nics.get(nic_to_service));
						if(queue_to_service.peek() != null){
							queue_to_service.routeBit();
							// send the packet if all the bits inside the packet have been routed.
							if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
								this.forwardPacket(queue_to_service.remove());
							}
							counter4weight = (counter4weight+1)%queue_to_service.getWeight();
						}else{//if the queue is already null
							counter4weight = 0;
						}
						lastNicServiced = nic_to_service;
					}
				}				
			}else{ // wrr  packet by packet
					//find the right queue to serve
				if(lastNicServiced == -1){ // treat the first round seperately
					queue_to_service = inputQueues.get(nics.get(0));
					if(queue_to_service.peek() != null){
						queue_to_service.routeBit();
						// send the packet if all the bits inside the packet have been routed.
						if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
							this.forwardPacket(queue_to_service.remove());
						}
						counter4weight++;
					}
					lastNicServiced = 0;
				}else{// if it is not the first round
					if(this.inputQueues.get(nics.get(lastNicServiced)).getBitsRoutedSinceLastPacketSent() != 0 || (!flag_switch)){//if this packet hasn't been finished
	
						// get the right queue to be serviced
						int temp_id = 0;
						for(int i=0;i<4;i++){
							if(this.inputQueues.get(this.nics.get(i)).peek() != null){
								if(temp_id == lastNicServiced){
									queue_to_service = this.inputQueues.get(nics.get(i));
								}
								temp_id++;
							}							
						}
						//route the bit
						queue_to_service.routeBit();
						counter4weight ++;
						// send the packet if all the bits inside the packet have been routed.
						if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
							if(counter4weight >= queue_to_service.getWeight()){//if the weights have been used out
								//switch the queue
								flag_switch = true;
								//reset the counter4weight
								counter4weight = 0;
							}
							this.forwardPacket(queue_to_service.remove());							
						}
						
					}else if(flag_switch ){// the packet has been finished and the flag is set.
						int id_to_service = (lastNicServiced+1)%(number_of_working_queue);
						// get the right queue to be serviced
						int temp_id = 0;
						for(int i=0;i<4;i++){
							if(this.inputQueues.get(this.nics.get(i)).peek() != null){
								if(temp_id == id_to_service){
									queue_to_service = this.inputQueues.get(nics.get(i));
								}
								temp_id++;
							}							
						}
						
						if(queue_to_service.peek() != null){// if there is still a packet in the queue
							//route the bit
							queue_to_service.routeBit();
							// send the packet if all the bits inside the packet have been routed.
							if(queue_to_service.peek().getSize() == queue_to_service.getBitsRoutedSinceLastPacketSent()){
								this.forwardPacket(queue_to_service.remove());
							}
						}
						//update the last serviced Nic  
						lastNicServiced =id_to_service;
						flag_switch = false;
						counter4weight ++;
					}				
				}
			}
		}
	}
	
	/**
	 * Perform weighted fair queuing on the queue
	 */
	private void wfq(){
		FIFOQueue queue_to_serve = null;
		
		//the scheduling part
		if(lastServicedQueue ==null){ // first round
		//check the first packet of all the working queues,choose the queue which has the lowest finishTime
		double lowest = 10000.0;

			for(IPNIC ipnic: inputQueues.keySet()){
				if(inputQueues.get(ipnic).peek()!=null){ 
					if(lowest > inputQueues.get(ipnic).peek().getFinishTime()){
						queue_to_serve=inputQueues.get(ipnic);
						lowest = inputQueues.get(ipnic).peek().getFinishTime();
					}
				}
			}
		}
		else{// second and the round after
			if(lastServicedQueue.getBitsRoutedSinceLastPacketSent() !=0){ // if the packet hasn't been sent out, the scheduler won't move on
				queue_to_serve = lastServicedQueue;
			}else{//the packet has finished, we need find the new queue to serve
				double lowest = 10000.0;

				for(IPNIC ipnic: inputQueues.keySet()){
					if(inputQueues.get(ipnic).peek()!=null){ 
						if(lowest > inputQueues.get(ipnic).peek().getFinishTime()){
							queue_to_serve=inputQueues.get(ipnic);
							lowest = inputQueues.get(ipnic).peek().getFinishTime();
						}
					}
				}
			}
			
		}
		
		//the routing part
		if((queue_to_serve !=null)){
				queue_to_serve.routeBit();
				if(queue_to_serve.peek().getSize() == queue_to_serve.getBitsRoutedSinceLastPacketSent()){
					this.forwardPacket(queue_to_serve.remove());
				}
				lastServicedQueue = queue_to_serve;
		}
		
	}
	
	/**
	 * adds a nic to the consumer 
	 * @param nic the nic to be added
	 */
	public void addNIC(IPNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * sets the weight of queues, used when a weighted algorithm is used.
	 * Example
	 * Nic A = 1
	 * Nic B = 4
	 * 
	 * For every 5 bits of service, A would get one, B would get 4.
	 * @param nic the nic queue to set the weight of
	 * @param weight the weight of the queue
	 */
	public void setQueueWeight(IPNIC nic, int weight){
		if(this.inputQueues.containsKey(nic))
			this.inputQueues.get(nic).setWeight(weight);
		
		else System.err.println("(IPRouter) Error: The given NIC does not have a queue associated with it");
	}
	
	/**
	 * moves time forward 1 millisecond
	 */
	public void tock(){
		this.time+=1;
		
		// Add 1 delay to all packets in queues
		ArrayList<FIFOQueue> delayedQueues = new ArrayList<FIFOQueue>();
		for(Iterator<FIFOQueue> queues = this.inputQueues.values().iterator(); queues.hasNext();){
			FIFOQueue queue = queues.next();
			if(!delayedQueues.contains(queue)){
				delayedQueues.add(queue);
				queue.tock();
			}
		}
		
		// calculate the new virtual time for the next round
		if(this.wfq){
			int weights = 0;
			for(IPNIC ipnic: inputQueues.keySet()){//accumulate all the weights of non-empty queues. 
				if(inputQueues.get(ipnic).peek()!=null){
					weights+= inputQueues.get(ipnic).getWeight();
				}
			}
			
			this.virtualTime += 1.0/weights;//update the virtual time,1 is the line speed: 1bit/round
			//System.out.println(this.virtualTime);
		}
		
		// route bit for this round
		this.routeBit();
	}
	
	/**
	 * set the router to use FIFO service
	 */
	public void setIsFIFO(){
		this.fifo = true;
		this.rr = false;
		this.wrr = false;
		this.wfq = false;
	
		// Setup router for FIFO under here
		this.fifo_queue = new FIFOQueue();
	}
	
	/**
	 * set the router to use Round Robin service
	 */
	public void setIsRoundRobin(){
		this.fifo = false;
		this.rr = true;
		this.wrr = false;
		this.wfq = false;
		
		// Setup router for Round Robin under here
		for(IPNIC ipnic: nics){//create a queue for each nic
			inputQueues.put(ipnic, new FIFOQueue());
		}
	}
	
	/**
	 * sets the router to use weighted round robin service
	 */
	public void setIsWeightedRoundRobin(){
		this.fifo = false;
		this.rr = false;
		this.wrr = true;
		this.wfq = false;
		
		// Setup router for Weighted Round Robin under here
		for(IPNIC ipnic: nics){//create a queue for each nic
			inputQueues.put(ipnic, new FIFOQueue());
		}
	}
	
	/**
	 * sets the router to use weighted fair queuing
	 */
	public void setIsWeightedFairQueuing(){
		this.fifo = false;
		this.rr = false;
		this.wrr = false;
		this.wfq = true;
		
		// Setup router for Weighted Fair Queuing under here
		for(IPNIC ipnic: nics){//create a queue for each nic
			inputQueues.put(ipnic, new FIFOQueue());
		}
	}
	
	/**
	 * sets if the router should route bit-by-bit, or entire packets at a time
	 * @param	routeEntirePacket if the entire packet should be routed
	 */
	public void setRouteEntirePacket(Boolean routeEntirePacket){
		this.routeEntirePacket=routeEntirePacket;
	}
}
