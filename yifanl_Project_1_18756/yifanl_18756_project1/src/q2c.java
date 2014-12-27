import DataTypes.SONETFrame;
import DataTypes.SPE;
import NetworkElements.OpticalNICTA;
import NetworkElements.OtoOLink;
import NetworkElements.SONETRouter;


public class q2c {
	public void twoRings(){
		// setup the network 
		System.out.println("Setting up two routers");
		// Create three SONET routers
		SONETRouter router1 = new SONETRouter("00:11:22");
		SONETRouter router2 = new SONETRouter("88:77:66");
		SONETRouter router3 = new SONETRouter("33:44:55");
		
		// tell routers a wavelength to add/drop on (in this case their own frequencies)
		router1.addDropWavelength(1310);
		router2.addDropWavelength(1490);
		router3.addDropWavelength(1550);
		
		// tell router 1 the wavelength each router is add/dropping on
		router1.addDestinationFrequency("00:11:22", 1310);
		router1.addDestinationFrequency("88:77:66", 1490);
		router1.addDestinationFrequency("33:44:55", 1550);
		// tell router 2 the wavelength each router is add/dropping on
		router2.addDestinationFrequency("00:11:22", 1310);
		router2.addDestinationFrequency("88:77:66", 1490);
		router2.addDestinationFrequency("33:44:55", 1550);
		// tell router 3 the wavelength each router is add/dropping on
		router3.addDestinationFrequency("00:11:22", 1310);
		router3.addDestinationFrequency("88:77:66", 1490);
		router3.addDestinationFrequency("33:44:55", 1550);

		// Create an interface for each router
		OpticalNICTA nicRouter11 = new OpticalNICTA(router1);
		nicRouter11.setID(11);
		nicRouter11.setIsWorkingNIC(true);
		OpticalNICTA nicRouter12 = new OpticalNICTA(router1);
		nicRouter12.setID(12);
		nicRouter12.setIsWorkingNIC(false);
		OpticalNICTA nicRouter21 = new OpticalNICTA(router2);
		nicRouter21.setID(21);
		nicRouter21.setIsWorkingNIC(true);
		OpticalNICTA nicRouter22 = new OpticalNICTA(router2);
		nicRouter22.setID(22);
		nicRouter22.setIsWorkingNIC(false);
		
		nicRouter11.setIsWorking(nicRouter12);
		nicRouter12.setIsProtection(nicRouter11);	
		nicRouter21.setIsWorking(nicRouter22);
		nicRouter22.setIsProtection(nicRouter21);
		
		OpticalNICTA nicRouter13 = new OpticalNICTA(router1);
		nicRouter13.setID(13);
		nicRouter13.setIsWorkingNIC(true);
		OpticalNICTA nicRouter14 = new OpticalNICTA(router1);
		nicRouter14.setID(14);
		nicRouter14.setIsWorkingNIC(false);
		OpticalNICTA nicRouter31 = new OpticalNICTA(router3);
		nicRouter31.setID(31);
		nicRouter31.setIsWorkingNIC(true);
		OpticalNICTA nicRouter32 = new OpticalNICTA(router3);
		nicRouter32.setID(32);
		nicRouter32.setIsWorkingNIC(false);
		
		nicRouter13.setIsWorking(nicRouter14);
		nicRouter14.setIsProtection(nicRouter13);
		nicRouter31.setIsWorking(nicRouter32);
		nicRouter32.setIsProtection(nicRouter31);
		
		OpticalNICTA nicRouter23 = new OpticalNICTA(router2);
		nicRouter23.setID(23);
		nicRouter23.setIsWorkingNIC(true);
		OpticalNICTA nicRouter24 = new OpticalNICTA(router2);
		nicRouter24.setID(24);
		nicRouter24.setIsWorkingNIC(false);
		OpticalNICTA nicRouter33 = new OpticalNICTA(router3);
		nicRouter33.setID(33);
		nicRouter33.setIsWorkingNIC(true);
		OpticalNICTA nicRouter34 = new OpticalNICTA(router3);
		nicRouter34.setID(34);
		nicRouter34.setIsWorkingNIC(false);
		
		nicRouter23.setIsWorking(nicRouter24);
		nicRouter24.setIsProtection(nicRouter23);
		nicRouter33.setIsWorking(nicRouter34);
		nicRouter34.setIsProtection(nicRouter33);

		
		// Create four-uni directional links between the routers
		 //links between router 1 and router 2
		OtoOLink OneToTwo1 = new OtoOLink(nicRouter11, nicRouter21);
		OtoOLink TwoToOne1 = new OtoOLink(nicRouter21, nicRouter11);
		OtoOLink OneToTwo2 = new OtoOLink(nicRouter12, nicRouter22);
		OtoOLink TwoToOne2 = new OtoOLink(nicRouter22, nicRouter12);
		
		OneToTwo1.cutLink();
		TwoToOne1.cutLink();
//		OneToTwo2.cutLink();
//		TwoToOne2.cutLink();
		
		
		 //links between router 1 and router 3
		OtoOLink OneToThree1 = new OtoOLink(nicRouter13, nicRouter31);
		OtoOLink ThreeToOne1 = new OtoOLink(nicRouter31, nicRouter13);
		OtoOLink OneToThree2 = new OtoOLink(nicRouter14, nicRouter32);
		OtoOLink ThreeToOne2 = new OtoOLink(nicRouter32, nicRouter14);
		 //links between router 2 and router 3
		OtoOLink TwoToThree1 = new OtoOLink(nicRouter23, nicRouter33);
		OtoOLink ThreeToTwo1 = new OtoOLink(nicRouter33, nicRouter23);
		OtoOLink TwoToThree2 = new OtoOLink(nicRouter24, nicRouter34);
		OtoOLink ThreeToTwo2 = new OtoOLink(nicRouter34, nicRouter24);
//	TwoToThree1.cutLink();
//	ThreeToTwo1.cutLink();
		TwoToThree2.cutLink();ThreeToTwo2.cutLink();
//		TwoToThree1.cutLink();ThreeToTwo1.cutLink();
		/*
		 * Sent a frame on the network
		 */
		router1.source(new SONETFrame(new SPE(0)), 1490);


	}
	
	public static void main(String args[]){
		q2c go = new q2c();
		go.twoRings();
	}
}
