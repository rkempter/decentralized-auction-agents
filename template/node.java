package template;

/* import table */
import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class node {

	private node parent;
	private ArrayList<node> children;
	private ArrayList<ArrayList<Object>> nodeState;
	private int capacity;
	private double costs;
	private Vehicle vehicle;
	private City nodeCity;

	/**
	 * Constructor of a node
	 * 
	 * @param _vehicle
	 * @param _nodeCity
	 * @param _nodeState
	 * @param _capacity
	 * @param _costs
	 * @param _parent
	 * @param _hashTable
	 * @param alg
	 */
	public node(Vehicle _vehicle, City _nodeCity, ArrayList<ArrayList<Object>> _nodeState, int _capacity, double _costs, node _parent){
		nodeState = _nodeState;
		vehicle = _vehicle;
		capacity = _capacity;
		costs = _costs;
		parent = _parent;
		nodeCity = _nodeCity;
		children= new ArrayList<node>();
	}

	/**
	 * Expands the current node
	 * 
	 * @return ArrayList<node> with all its children nodes
	 */
	public ArrayList<node> expandNodes() {
		ArrayList<node> childNodes = new ArrayList<node>();
		ArrayList<Integer> subState = createSubState(nodeState);

		for (int i=0; i< subState.size(); i++){
			node newState = createNewState(subState.get(i));
			if(newState != null){
				childNodes.add(newState);
			}
		}

		return childNodes;
	}

	/**
	 * Computes all the possible children states of the current node
	 * 
	 * @param currentState
	 * @return ArrayList<Integer>
	 */
	private ArrayList<Integer> createSubState(ArrayList<ArrayList<Object>> currentState) {
		ArrayList<Integer> subState = new ArrayList<Integer>();

		for(int i = 0; i < currentState.size(); i++) {
			Integer pos = new Integer(i);
			actionStates currentActionState = (actionStates) currentState.get(i).get(1);
			if(currentActionState == actionStates.INITSTATE || currentActionState == actionStates.PICKEDUP) {
				subState.add(pos);
			}
		}

		return subState;
	}

	/**
	 * Creates a new state
	 * 
	 * @param selectedTaskIndex
	 * @return
	 */
	private node createNewState(Integer selectedTaskIndex) {
		ArrayList<ArrayList<Object>> newState= new ArrayList<ArrayList<Object>>();
		node child = null;
		ArrayList<Object> currentTaskNode = nodeState.get(selectedTaskIndex);
		actionStates currentActionState = (actionStates) currentTaskNode.get(1);

		for(int i=0; i<nodeState.size(); i++){
			newState.add(new ArrayList<Object>());
			newState.get(i).add(nodeState.get(i).get(0));
			newState.get(i).add(nodeState.get(i).get(1));			
		}

		if(currentActionState == actionStates.PICKEDUP) {		 //selected task is PICKEDUP
			child = calculateNewStateParameters(newState.get(selectedTaskIndex), newState);
		} else if(capacity >= ((Task) currentTaskNode.get(0)).weight) {
			child = calculateNewStateParameters(newState.get(selectedTaskIndex), newState);
		}
		return child;
	}
	/**
	 * Generates a new child based on the newState and the node we modify.
	 * 
	 * @param currentTaskNode
	 * @param newState
	 * @return planNode
	 */

	private node calculateNewStateParameters(ArrayList<Object> currentTaskNode, ArrayList<ArrayList<Object>> newState) {
		node child = null;
		Task currentTaskNodeTask = (Task) currentTaskNode.get(0);
		actionStates currentActionState = (actionStates) currentTaskNode.get(1);
		double newCost = calculateCost(currentTaskNode, currentTaskNodeTask, currentActionState);
		int newCapacity = calculateCapacity(capacity, currentTaskNodeTask, currentActionState);
		
		if(currentActionState == actionStates.PICKEDUP) {
			child = new node(vehicle, currentTaskNodeTask.pickupCity, newState, newCapacity, newCost, this);
		}
		else {
			child = new node(vehicle, currentTaskNodeTask.deliveryCity, newState, newCapacity, newCost, this);
		}
		children.add(child);
		
		return child;
	}

	/**
	 * Computes the costs for reaching a child node. This depends if we deliver a task or pick one up.
	 * Delivering a task returns a rewards.
	 * 
	 * @param currentTaskNode
	 * @param currentTaskNodeTask
	 * @param taskState
	 * @return
	 */
	private double calculateCost(ArrayList<Object> currentTaskNode, Task currentTaskNodeTask, actionStates taskState) {
		double newCost = 0;
		if(taskState == actionStates.PICKEDUP) {
			deliverTask(currentTaskNode, currentTaskNodeTask.deliveryCity);
			newCost = costs + (nodeCity.distanceTo(currentTaskNodeTask.deliveryCity) * vehicle.costPerKm());
		} else if (taskState == actionStates.INITSTATE){
			currentTaskNode.set(1, actionStates.PICKEDUP);
			newCost = costs + (nodeCity.distanceTo(currentTaskNodeTask.pickupCity) * vehicle.costPerKm());
		}	
		return newCost;
	}

	/**
	 * Computes the current capacity.
	 * Delivering a task gives more capacity, picking up a task reduces the capacity.
	 * 
	 * @param capacity
	 * @param currentTaskNodeTask
	 * @param taskState
	 * @return int
	 */
	private int calculateCapacity(int capacity, Task currentTaskNodeTask, actionStates taskState) {
		int newCapacity = 0;
		if(taskState == actionStates.PICKEDUP) {
			newCapacity = capacity - currentTaskNodeTask.weight;
		} else {
			newCapacity = capacity + currentTaskNodeTask.weight;
		}
		return newCapacity;
	}

	/**
	 * Sets the current task as delivered
	 * 
	 * @param currentTaskNode
	 * @param city
	 */
	private void deliverTask(ArrayList<Object> currentTaskNode, City city) {
		if( ((Task) currentTaskNode.get(0)).deliveryCity == city && (actionStates) currentTaskNode.get(1) == actionStates.PICKEDUP) {
			currentTaskNode.set(1, actionStates.DELIVERED);
		}
	}

	/**
	 * Getter and Setter methods
	 */

	public node getParent() {
		return parent;
	}

	public double getCosts() {
		return costs;
	}

	public int getCostsPerKm() {
		return vehicle.costPerKm();
	}

	public City getCity() {
		return nodeCity;
	}

	public void printState() {
		//System.out.println(nodeState);
	}
	
	public int getCapacity() {
		return capacity;
	}

	public ArrayList<ArrayList<Object>> getState() {
		return nodeState;
	}
}
