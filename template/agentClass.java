package template;

import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.task.Task;

public class agentClass {
	
	// Total cost last time
	private int lastCosts = 0;
	
	private Vehicle vehicle;
	
	// List of tasks for this agent
	private ArrayList<Task> taskList = new ArrayList<Task>();
	
	// Arraylist with task of opponents
	private ArrayList<ArrayList<Task>> opponentTaskList = new ArrayList<ArrayList<Task>>();
	
	// Arraylist with costs of opponents
	
	public agentClass(Vehicle _vehicle) {
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
	
	public void addTask(Task task) {
		taskList.add(task);
	}
	
	public void setLastCosts(int lastcosts) {
		this.lastCosts = lastcosts;
	}
	
	public ArrayList<node> getPathList() {
		System.out.println("ASTAR");
		Comparator<node> comparator = new nodeComparator();
		PriorityQueue<node> nodeQueue = new PriorityQueue<node> (1000, comparator);
		ArrayList<node> visitedNodes = new ArrayList<node>();
		currentNode = startNode;
		int i = 0;
		while(!checkGoalState(currentState)) {
			ArrayList<planeNode> childQueue = currentNode.expandNodes();
			nodeQueue.addAll(childQueue);
			try{
				currentNode = nodeQueue.remove();
//				System.out.println(currentNode.getState());
//				System.out.println(currentNode.getCity());
//				System.out.println(currentNode.getCapacity());
//				System.out.println("Estimated total cost: "+currentNode.getCosts()+planNodeComparator.getHeuristicCost(currentNode.getState(), vehicle.costPerKm(), currentNode.getCapacity(), currentNode.getCity()));
			} catch (Exception e) {
				break;
			}
			
			visitedNodes.add(currentNode);
			currentState = currentNode.getState();
			i++;
		}
		System.out.println("Iteration: "+i);
		
		if(nodeQueue.size() > 0) {
			System.out.println("ASTAR: GOAL NODE REACHED!");
			goalNode = currentNode;
			
			// Do backtracking from goal node and create plan
			plan = backtrackingPlan(goalNode);
		} else {
			System.out.println("Node Queue is empty and we haven't found a solution");
		}

		break;
	}
	
	public int computeCosts() {
		ArrayList<ArrayList<Object>> state = generateInitState(this.taskList);
		
		
		
		
		
		
		while( checkGoalState(state) ) {
			
		}
		
		return 0;
	}
	

	
	public static boolean checkGoalState(ArrayList<ArrayList<Object>> states) {
		return true;
	}
}
