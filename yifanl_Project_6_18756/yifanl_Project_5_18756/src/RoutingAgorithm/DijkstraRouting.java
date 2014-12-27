package RoutingAgorithm;
/*
 * This class is designed to calculate the routing table of a list of LSR. The routing table is stored on each router.
 * 
 */
		
		
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import NetworkElements.LSR;

public class DijkstraRouting {
	private ArrayList<LSR> graph;
	
	public DijkstraRouting(ArrayList<LSR> graph){
		this.graph =graph;
	}
	
	public void BuildRoutingTable(){
		HashMap<LSR,Integer> dist = new HashMap<LSR,Integer>(); // the data structure stored with the shortest distance from LSR to the source node 
		HashMap<LSR,LSR> prev = new HashMap<LSR,LSR>(); // the data structure stored with the previous node in optimal path from source
		HashSet<LSR> Q = new HashSet<LSR>(); // unvisited nodes
		for(LSR l:this.graph){
			//for each router calculate all the routes where it can arrive
			
			dist.put(l,0);//distance from source to source
			for(LSR ltemp: this.graph){ // Dijkstra initialization
				if(ltemp != l){
					dist.put(ltemp, 99999); //99999 represents infinity here,unknown distance from source to ltemp
					prev.put(ltemp, null); //previous node for ltemp is undefined
				}
				Q.add(ltemp);
			}
			
			while(!Q.isEmpty()){  //the main loop
				
				int  d = 99999; 
				LSR u = null;
				for(LSR temp:Q){ //set u as the one in Q with min dist[u]			
					if(dist.get(temp) < d){
						u = temp;
						d =dist.get(u);
					}
				}
				
				Q.remove(u); // remove u from Q
				for(LSR v:u.getNeighbors()){ // v is a neighbor of u and has not yet been removed from Q
					int alt = dist.get(u) + 1;//1 is the distance between V and U
					if(alt < dist.get(v)){
						dist.remove(v);
						dist.put(v, alt); // dist[v] = alt
						prev.remove(v);
						prev.put(v,u); // prev[v] = u
 					}
				}				
			}
			//printing the result
//			for(LSR k :dist.keySet()){
//				System.out.println("Source:"+l.getAddress()+" Distance:"+dist.get(k)+"Des: "+k.getAddress());
//			}
			
			// end of the dijkstra algorithm for node l
			
			//building the routing table 
			for(LSR ltemp:this.graph){
				ArrayList<LSR> S = new ArrayList<LSR>();//the list to store the shortest path from source to ltemp
				if(ltemp != l){
					while(prev.get(ltemp)!=null){//trace back the shortest path from ltemp to l
						S.add(ltemp);
						ltemp = prev.get(ltemp);//traverse from target to source
					}
					
					l.getRoutingTable().put(S.get(0).getAddress(), l.getNicByLSR(S.get(S.size()-1)));
				}else{
					l.getRoutingTable().put(l.getAddress(), null);
				}
				
			}
			
			//clear the temporary variables
			dist.clear();
			prev.clear();
			Q.clear();
		}
	}
}
