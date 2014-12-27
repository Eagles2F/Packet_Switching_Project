import DataTypes.Packet;
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
		LSR rA = new LSR(1);
		rA.setLSRType(0);
		LSR rB = new LSR(2);
		rB.setLSRType(2);
		LSR rC = new LSR(3);
		rC.setLSRType(1);
		LSR rD = new LSR(4);
		rD.setLSRType(1);
		LSR rE = new LSR(5);
		rE.setLSRType(1);
		LSR rF = new LSR(6);
		rF.setLSRType(2);
		LSR rG = new LSR(7);
		rG.setLSRType(0);
		
		// give the routers interfaces
		LSRNIC rAn1 = new LSRNIC(rA);
		LSRNIC rBn1 = new LSRNIC(rB);
		LSRNIC rBn2 = new LSRNIC(rB);
		LSRNIC rBn3 = new LSRNIC(rB);
		LSRNIC rCn1 = new LSRNIC(rC);
		LSRNIC rCn2 = new LSRNIC(rC);
		LSRNIC rCn3 = new LSRNIC(rC);
		LSRNIC rCn4 = new LSRNIC(rC);
		LSRNIC rDn1 = new LSRNIC(rD);
		LSRNIC rDn2 = new LSRNIC(rD);
		LSRNIC rDn3 = new LSRNIC(rD);
		LSRNIC rDn4 = new LSRNIC(rD);
		LSRNIC rEn1 = new LSRNIC(rE);
		LSRNIC rEn2 = new LSRNIC(rE);
		LSRNIC rEn3 = new LSRNIC(rE);
		LSRNIC rEn4 = new LSRNIC(rE);
		LSRNIC rFn1 = new LSRNIC(rF);
		LSRNIC rFn2 = new LSRNIC(rF);
		LSRNIC rFn3 = new LSRNIC(rF);
		LSRNIC rGn1 = new LSRNIC(rG);
		
		// physically connect the router's nics
		OtoOLink lAB = new OtoOLink(rAn1, rBn1);
		OtoOLink lBC = new OtoOLink(rBn3, rCn3);
		OtoOLink lCD = new OtoOLink(rCn4, rDn3);
		OtoOLink lDE = new OtoOLink(rDn4, rEn3);
		OtoOLink lEF = new OtoOLink(rEn4, rFn3);
		OtoOLink lFG = new OtoOLink(rFn2, rGn1);
		
		OtoOLink lBCopt = new OtoOLink(rBn2, rCn1,true);// optical link
		OtoOLink lCDopt = new OtoOLink(rCn2, rDn1, true); // optical link
		OtoOLink lDEopt = new OtoOLink(rDn2, rEn1,true);// optical link
		OtoOLink lEFopt = new OtoOLink(rEn2, rFn1, true); // optical link
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(rA);
		this.allConsumers.add(rB);
		this.allConsumers.add(rC);
		this.allConsumers.add(rD);
		this.allConsumers.add(rE);
		this.allConsumers.add(rF);
		this.allConsumers.add(rG);
		
		//running the routing algorithm to generate the routing table
		DijkstraRouting dijk = new DijkstraRouting(this.allConsumers);
		dijk.BuildRoutingTable();
		
		//send packets from router 1 to the other routers...
		rA.createPacket(7);
		for(int i=0;i<30;i++){
			tock();
		}
		rG.createPacket(1);
		for(int i=0;i<10;i++){
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
			allConsumers.get(i).receivePackets();
		
	}	
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}