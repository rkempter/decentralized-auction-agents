package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicleList;
	private List<vehicleClass> vehicleObjectList;
	
	
	private int currentTaskToVehicle = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicleList = agent.vehicles();
		this.vehicleObjectList = new ArrayList<vehicleClass>();
		
		for(int i = 0; i < this.vehicleList.size(); i++) {
			vehicleClass newVehicleObject = new vehicleClass(this.vehicleList.get(i), distribution);
			this.vehicleObjectList.add(newVehicleObject);
			System.out.println("Vehicle starts here: "+this.vehicleList.get(i).getCurrentCity());
		}
		
		// create class for opponent
	}


	public void auctionResult(Task previous, int winner, Long[] bids) {
		
		for(int i=0; i< this.vehicleObjectList.size(); i++){
			System.out.println("-----------vehicle "+vehicleObjectList.get(i).getVehicle().id()+" --------");
			float ourBid;
			float opponentBid= 1000;		//fake bc no tournament
			if(agent.id()==1){
				ourBid= bids[1];
				opponentBid= bids[0];
			}
			else{
				ourBid= bids[0];
				System.out.println("---->"+ bids.length);
				System.out.println("---->"+ bids[0]);

				if(bids.length>1) opponentBid= bids[1];
			}
			this.vehicleObjectList.get(i).addBiddingHistory(ourBid, opponentBid);	
		}
		System.out.println("　　　　Our final bid is: "+ bids[0]);
		// For each opponent bid, 	
		if (winner == agent.id()) {
			this.vehicleObjectList.get(this.currentTaskToVehicle).acceptTask(previous);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		
		// Start computation for marginalCost
		
		double minOffer = 100000;
		int i = 0;
		for(i = 0; i < this.vehicleObjectList.size(); i++) {
			this.vehicleObjectList.get(i).addTask(task);
			double offer = this.vehicleObjectList.get(i).getOffer(task);
			System.out.println("The offer of vehicle "+i+" is: "+offer);
			if(offer <= minOffer) {
				minOffer = offer;
				this.currentTaskToVehicle = i;
			}
		}
		
		return (long) Math.round(minOffer);
	}

	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		List<Plan> plans = new ArrayList<Plan>();
		for(int i = 0; i < vehicles.size(); i++) {
			Plan newPlan = this.vehicleObjectList.get(i).getPath(tasks);
			plans.add(newPlan);
		}
		
		System.out.println("All plans created");

		return plans;
	}
	
	public static boolean checkGoalState(ArrayList<ArrayList<Object>> state) {
		int stateSize = state.size();
		for(int i = 0; i < stateSize; i++) {
			actionStates currentStateAction = (actionStates) state.get(i).get(1);
			if(currentStateAction != actionStates.DELIVERED) {
				return false;
			}
		}
		return true;
	}
}
