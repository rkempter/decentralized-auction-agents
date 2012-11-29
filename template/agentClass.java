package template;

import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.task.Task;

public class agentClass {
	
	// Total cost last time
	
	private Vehicle vehicle;
	
	// List of tasks for this agent
	private ArrayList<Task> taskList = new ArrayList<Task>();
	
	// Arraylist with task of opponents
	private ArrayList<ArrayList<Task>> opponentTaskList = new ArrayList<ArrayList<Task>>();
	
	// Arraylist with costs of opponents
	
	
	public agentClass(Vehicle _vehicle) {
		this.vehicle = _vehicle;
	}
	
	public static boolean checkGoalState(ArrayList<ArrayList<Object>> states) {
		return true;
	}
	
	public void addTask(Task task) {
		taskList.add(task);
	}
	
	public int computeCosts() {
		
		
		return 0;
	}
}
