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
	private Random random;
	private List<Vehicle> vehicleList;
	private City currentCity;
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
			vehicleClass newVehicleObject = new vehicleClass(this.vehicleList.get(i));
			this.vehicleObjectList.add(newVehicleObject);
			System.out.println("Vehicle starts here: "+this.vehicleList.get(i).getCurrentCity());
		}
	}


	public void auctionResult(Task previous, int winner, Long[] bids) {		
		if (winner == agent.id()) {
			this.vehicleObjectList.get(this.currentTaskToVehicle).acceptTask();
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
		System.out.println("-------------");
		System.out.println("Vehicle "+this.currentTaskToVehicle+" gets the task!");
		System.out.println("Vehicle 0 has the following tasklist: "+this.vehicleObjectList.get(0).getTaskList());
		System.out.println("Vehicle 1 has the following tasklist: "+this.vehicleObjectList.get(1).getTaskList());
		System.out.println("Offer: "+minOffer);

		return (long) Math.round(minOffer);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		
		List<Plan> plans = new ArrayList<Plan>();
		for(int i = 0; i < vehicles.size(); i++) {
			Plan newPlan = this.vehicleObjectList.get(i).getPath();
			plans.add(newPlan);
		}

		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
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
