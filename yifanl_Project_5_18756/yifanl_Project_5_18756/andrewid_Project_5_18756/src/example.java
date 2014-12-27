import NetworkElements.*;
import RoutingAgorithm.DijkstraRouting;

import java.util.*;

public class example {
	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	/**
	 * Create a network and creates connections
	 * @since 1.0
	 */
	public void go(){
		System.out.println("** SYSTEM SETUP **");
		
		// Create some new ATM Routers
		LSR r1 = new LSR(9);
		LSR r2 = new LSR(3);
		//LSR r3 = new LSR(11);
		//LSR r4 = new LSR(13);
		//LSR r5 = new LSR(14);
		
		// give the routers interfaces
		LSRNIC r1n1 = new LSRNIC(r1);
		LSRNIC r2n1 = new LSRNIC(r2);
	//	LSRNIC r2n2 = new LSRNIC(r2);
	//	LSRNIC r2n3 = new LSRNIC(r2);
	//	LSRNIC r3n1 = new LSRNIC(r3);
	//	LSRNIC r4n1 = new LSRNIC(r4);
	//	LSRNIC r4n2 = new LSRNIC(r4);
	//	LSRNIC r5n1 = new LSRNIC(r5);
		
		// physically connect the router's nics
		OtoOLink l1 = new OtoOLink(r1n1, r2n1);
//		OtoOLink l2 = new OtoOLink(r2n2, r3n1);
//		OtoOLink l3 = new OtoOLink(r2n3, r4n1);
//		OtoOLink l4 = new OtoOLink(r4n2, r5n1);
			
		// Add the objects that need to move in time to an array
		this.allConsumers.add(r1);
		this.allConsumers.add(r2);
//		this.allConsumers.add(r3);
//		this.allConsumers.add(r4);
//		this.allConsumers.add(r5);
		
		//running the routing algorithm to generate the routing table
		DijkstraRouting dijk = new DijkstraRouting(this.allConsumers);
		dijk.BuildRoutingTable();
		
		
		//send packets from router 1 to the other routers...
		//r1.createPacket(11, 0);
		//r1.createPacket(3, 0);
		//r1.createPacket(11, 0);
		//r1.createPacket(13, 0);
		//r1.createPacket(14, 0);
		
		for(int i=0;i<15;i++){
			for(int j=0;j<40;j++){// BE traffic
				r1.createPacket(3, 0);
			}
			for(int j=0;j<5;j++){// EF traffic
				r1.createPacket(3, 1);
			}
			for(int j=0;j<3;j++){// AF-Class1-Drop Pref 1
				r1.createPacket(3, 211);
			}
			for(int j=0;j<2;j++){// AF-Class1-Drop Pref 2
				r1.createPacket(3, 212);
			}
			for(int j=0;j<3;j++){// AF-Class2-Drop Pref 1
				r1.createPacket(3, 221);
			}
			for(int j=0;j<2;j++){// AF-Class2-Drop Pref 2
				r1.createPacket(3, 222);
			}
			for(int j=0;j<4;j++){// AF-Class3-Drop Pref 1
				r1.createPacket(3, 231);
			}
			for(int j=0;j<5;j++){// AF-Class3-Drop Pref 2
				r1.createPacket(3, 232);
			}
			tock();
		}
	}
	
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;
		
		
		// Send packets between routers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).sendPackets();

		// Move packets from input buffers to output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).recievePackets();
		
	}
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}