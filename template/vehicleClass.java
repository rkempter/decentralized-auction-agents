package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class vehicleClass {
	
	// Total cost last time
	private double lastCosts = 0;
	
	// List of visited nodes the last time
	private ArrayList<node> lastVisitedNodes = null;
	private ArrayList<node> intermediateVisitedNodes = null;
	
	// Last accepted goal state
	private node lastGoalNode = null;
	// Last (not yet accepted) achieved goal state
	private node intermediateGoalNode = null;
	
	private double intermediateCosts = 0;
	
	// The vehicle
	private Vehicle vehicle;
	
	// List of tasks for this agent
	private ArrayList<Task> taskList = new ArrayList<Task>();
	
	private ArrayList<Task> intermediateTaskList = new ArrayList<Task>();
	
	// Arraylist with task of opponents
	private ArrayList<ArrayList<Task>> opponentTaskList = new ArrayList<ArrayList<Task>>();
	
	// Arraylist with costs of opponents
	
	public vehicleClass(Vehicle _vehicle) {
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
			System.out.println("State: "+currentState);
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
			
			long distance = computeTaskDistance(newProposedTask);
			System.out.println("Distance is: "+distance);
			
			return marginalCosts / distance;

		} else {
			System.out.println("Node Queue is empty and we haven't found a solution");
			
			return 10000;
		}
	}
	
	private long computeTaskDistance(Task task) {
		City pickupCity = task.pickupCity;
		
		return (long) pickupCity.distanceTo(task.deliveryCity);
	}
	
	public void acceptTask() {
		this.lastCosts = this.intermediateCosts;
		this.lastGoalNode = this.intermediateGoalNode;
		this.lastVisitedNodes = cloneNodeList(this.intermediateVisitedNodes);
		this.taskList = cloneTaskList(this.intermediateTaskList);
	}
	
	public Plan getPath(TaskSet tasks) {
		// Do backtracking
		node currentNode = this.lastGoalNode;
		ArrayList<node> path = new ArrayList<node>();

		while(currentNode != null) {
			currentNode.printState();
			path.add(currentNode);
			currentNode = currentNode.getParent();	
		}
		
		Collections.reverse(path);
		System.out.println("Path: "+path);
		
		City current = path.get(0).getCity();
		
		Plan optimalPlan = new Plan(current);
		System.out.println("Startcity: "+current);
		
		for(int i = 1; i < path.size() ; i++) {
			currentNode = path.get(i);
			City nextCity = currentNode.getCity();
			for(City city : current.pathTo(nextCity)) {
				System.out.println("City: "+city);
				optimalPlan.appendMove(city);
			}
			
			int stateSize = currentNode.getState().size();
			for(int j=0; j< stateSize; j++) {
				actionStates currentNodeAction = (actionStates) currentNode.getState().get(j).get(1);
				actionStates lastNodeAction = (actionStates) path.get(i-1).getState().get(j).get(1);
				if(currentNodeAction != lastNodeAction) {
					Task currentTask = (Task) path.get(i).getState().get(j).get(0);

					switch(currentNodeAction) {
					case PICKEDUP:
						System.out.println("Pickup: "+currentTask);
						optimalPlan.appendPickup(currentTask);
						break;
					case DELIVERED:
						System.out.println("Deliver: "+currentTask);
						optimalPlan.appendDelivery(currentTask);
						break;
					default:
						System.out.println("_OO_");
					}
				}
			}
			current = nextCity;
		}
		System.out.println(optimalPlan);
		return optimalPlan;
	}
	
	public Vehicle getVehicle() {
		return this.vehicle;
	}
	
	public ArrayList<Task> getTaskList() {
		return this.taskList;
	}
}
