package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.lang.Math;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class vehicleClass {
	//variable that represents the beginning reward (expressed as percentage of marginal cost )
	private static final double STARTING_INCOMING = .1;
	private static final double MAXIMUM_INCOMING= .7;
	private static final double MINIMUM_INCOMING= .05;
	
	
	private int winCounter = 0;
	private int previousHighPotentialTasks = 0;
	private float opponentMinimalBid = 1250;
	
	
	private ArrayList<Float> biddingHistory= new ArrayList<Float>();
	private double actualIncoming= STARTING_INCOMING;
	private int timeSinceLastChange= 0;
	// Total cost last time
	private double lastCosts = 0;

	// List of visited nodes the last time
	private ArrayList<node> lastVisitedNodes = new ArrayList<node>();
	private ArrayList<node> intermediateVisitedNodes = new ArrayList<node>();

	// Last accepted goal state
	private node lastGoalNode = null;
	// Last (not yet accepted) achieved goal state
	private node intermediateGoalNode = null;

	private Task intermediateTask = null;
	private double intermediateCosts = 0;

	// The vehicle
	private Vehicle vehicle;

	private TaskDistribution distribution = null;

	// Variable to check if we get through more better cities (better = higher task pickup probability)
	private int lastTaskPickupEstimation = -100;

	// List of tasks for this agent
	private ArrayList<Task> taskList = new ArrayList<Task>();

	private ArrayList<Task> intermediateTaskList = new ArrayList<Task>();

	// Arraylist with costs of opponents
	private float currentIncoming;

	public vehicleClass(Vehicle _vehicle, TaskDistribution _distribution) {
		this.distribution = _distribution;
		this.vehicle = _vehicle;
	}

	private ArrayList<ArrayList<Object>> generateInitState(ArrayList<Task> tasks) {
		ArrayList<ArrayList<Object>> state = new ArrayList<ArrayList<Object>>();
		for(int i = 0; i < tasks.size(); i++) {
			ArrayList<Object> taskState = new ArrayList<Object>();
			taskState.add(tasks.get(i));
			taskState.add(actionStates.INITSTATE);
			state.add(taskState);
		}

		return state;
	}

	private ArrayList<Task> cloneTaskList(ArrayList<Task> list) {
		ArrayList<Task> returnList = new ArrayList<Task>();

		for(int i = 0; i < list.size(); i++) {
			returnList.add(list.get(i));
		}

		return returnList;
	}

	private ArrayList<node> cloneNodeList(ArrayList<node> list) {
		ArrayList<node> returnList = new ArrayList<node>();

		for(int i = 0; i < list.size(); i++) {
			returnList.add(list.get(i));
		}

		return returnList;
	}

	public void addTask(Task task) {
		this.intermediateTask = task;
		this.intermediateTaskList.clear();
		this.intermediateTaskList = cloneTaskList(this.taskList);
		this.intermediateTaskList.add(task);
	}

	public double getOffer(Task newProposedTask) {
		Comparator<node> comparator = new nodeComparator();
		PriorityQueue<node> nodeQueue = new PriorityQueue<node> (1000, comparator);
		ArrayList<node> visitedNodes = new ArrayList<node>();
		ArrayList<ArrayList<Object>> currentState = generateInitState(this.intermediateTaskList);
		node currentNode = new node(vehicle, vehicle.getCurrentCity(), currentState, vehicle.capacity(), 0, null);
		visitedNodes.add(currentNode);
		int i = 0;

		while(!AuctionTemplate.checkGoalState(currentState)) {
			//System.out.println("State: "+currentState);
			ArrayList<node> childQueue = currentNode.expandNodes();
			nodeQueue.addAll(childQueue);
			try{
				currentNode = nodeQueue.remove();
			} catch (Exception e) {
				break;
			}

			visitedNodes.add(currentNode);
			currentState = currentNode.getState();
			i++;
		}

		System.out.println("Iteration: "+i);

		if(AuctionTemplate.checkGoalState(currentState)) {
			System.out.println("ASTAR: GOAL NODE REACHED!");
			double newCosts = currentNode.getCosts();
			double marginalCosts = newCosts - this.lastCosts;
			System.out.println("Marginal costs are: "+marginalCosts);

			// Compute the costs of the opponent
			// Adjust our offer

			this.intermediateGoalNode = currentNode;
			this.intermediateVisitedNodes = visitedNodes;
			this.intermediateCosts = newCosts;

			double offer = marginalCosts;
			if(offer <= 0) {
				offer = this.opponentMinimalBid < 750 ? 750 : this.opponentMinimalBid;
			}
			
			offer = adjustUsingDistribution(offer);
			
			return adjustBiddingOffer(offer);

		} else {
			System.out.println("Node Queue is empty and we haven't found a solution");
			return 10000;
		}
	}

	/**
	 * Create an ArrayList with the Cities on the Path (in right order)
	 * @return
	 */
	private ArrayList<City> createCityPath() {
		node currentNode = this.lastGoalNode;
		ArrayList<City> path = new ArrayList<City>();

		if(currentNode != null) { 
			City currentCity = currentNode.getCity();
			path.add(currentCity);

			while(currentNode.getParent() != null) {
				City nextCity = currentNode.getParent().getCity();
				for(City city : currentCity.pathTo(nextCity)) {
					path.add(city);
				}
				currentNode = currentNode.getParent();
			}

			Collections.reverse(path);
		}
		return path;
	}


	/**
	 * Uses the task distribution to check if there are paths on the way that have high probability of tasks
	 * 
	 * @param offer
	 * @param opponentOffers
	 * @return
	 */
	private double adjustUsingDistribution(double offer) {

		ArrayList<City> cityPath = createCityPath();

		int lastCounter = this.previousHighPotentialTasks;
		int counter = 0;

		for(int i = 0; i < cityPath.size(); i++) {
			for(int j = i+1; j < cityPath.size(); j++) {
				if(this.distribution.probability(cityPath.get(i), cityPath.get(j)) > 0.15) {
					//	cityPath.get(i).hashCode()+cityPath.get(j).hashCode()
					// Use hashtable to count routes only once?
					counter++;
				}
			}
		}
		
		this.previousHighPotentialTasks = counter;
		int difference = lastCounter - counter;
		
		double adjustedOffer = (1 - ((1/(1+Math.exp(-1/2*difference)) - 0.5)*0.2)) * offer;

		//System.out.println("Counter: "+counter);

		return adjustedOffer;
	}

	public void acceptTask(Task task) {
		this.lastCosts = this.intermediateCosts;
		this.lastGoalNode = this.intermediateGoalNode;
		this.lastVisitedNodes = cloneNodeList(this.intermediateVisitedNodes);
		this.taskList.add(task);
	}

	public Plan getPath(TaskSet tasks) {
		// Do backtracking
		node currentNode = this.lastGoalNode;
		ArrayList<node> path = new ArrayList<node>();

		int capacity = this.vehicle.capacity();
		System.out.println("Capacity vehicle: "+capacity);

		while(currentNode != null) {
			currentNode.printState();
			path.add(currentNode);
			currentNode = currentNode.getParent();	
		}

		Collections.reverse(path);
		Plan optimalPlan = Plan.EMPTY;
		System.out.println("Pathlength: "+path.size());

		if(path.size() != 0) {
			City current = path.get(0).getCity();

			optimalPlan = new Plan(current);

			//System.out.println("Startcity: "+current);
			System.out.println("Tasklist: "+ this.taskList);
			for(int i = 1; i < path.size() ; i++) {
				currentNode = path.get(i);
				City nextCity = currentNode.getCity();
				for(City city : current.pathTo(nextCity)) {
					//System.out.println("City: "+city);
					optimalPlan.appendMove(city);
				}

				int stateSize = currentNode.getState().size();
				//System.out.println("--------------------");
				for(int j=0; j< stateSize; j++) {
					actionStates currentNodeAction = (actionStates) currentNode.getState().get(j).get(1);
					actionStates lastNodeAction = (actionStates) path.get(i-1).getState().get(j).get(1);
					if(currentNodeAction != lastNodeAction) {
						Task currentTask = (Task) path.get(i).getState().get(j).get(0);

						System.out.println("CurrentTask: "+currentTask);

						Task currentTaskFromTaskSet = getTaskFromTaskSet(currentTask.id, tasks);

						switch(currentNodeAction) {
						case PICKEDUP:
							optimalPlan.appendPickup(currentTaskFromTaskSet);
							capacity -= currentTaskFromTaskSet.weight;
							break;
						case DELIVERED:
							optimalPlan.appendDelivery(currentTaskFromTaskSet);
							capacity += currentTaskFromTaskSet.weight;
							break;
						default:
							System.out.println("_OO_");
						}
					}
				}
				current = nextCity;
			}
		}
		System.out.println(optimalPlan);
		return optimalPlan;
	}


	public Task getTaskFromTaskSet(int id, TaskSet tasks) {
		for(int i = 0; i < tasks.size(); i++) {
			Task currentTask = (Task) tasks.toArray()[i];
			if (currentTask.id == id) {
				return currentTask;
			}
		}
		return null;
	}

	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public ArrayList<Task> getTaskList() {
		return this.taskList;
	}
	//add bids to the history of bidding
	public void addBiddingHistory(float ourBid, float opponentBid, boolean win) {
		if (!win && opponentBid < this.opponentMinimalBid) {
			this.opponentMinimalBid = opponentBid;
		}
		if(true == win) {
			float result = opponentBid / ourBid;
			this.winCounter++;
			this.biddingHistory.add(result);
		} else {
			this.winCounter = 0;
			this.biddingHistory = new ArrayList();
		}
	}
	
	private double adjustBiddingOffer(double offer) {
		float sum = 0;
		float average = 1;
		int x = this.winCounter;
		int i = 0;
		for(; i < this.biddingHistory.size(); i++) {
			sum += this.biddingHistory.get(i);
		}
		
		if(sum > 0) {
			average = sum / i;
		}
		
		double adjustedAverage = ((x+1) / (x+5) * Math.abs(1-average)) + 1;
		
		System.out.println("AdjustedAverage with which we multiply the offer "+adjustedAverage);
		
		return (adjustedAverage * offer);
	}
}
