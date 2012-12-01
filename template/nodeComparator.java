package template;

import java.util.ArrayList;
import java.util.Comparator;
import logist.task.Task;
import logist.topology.Topology.City;

public class nodeComparator implements Comparator<node>{


	/**
	 * Compares two nodes and checks which one has the higher reward.
	 * 
	 * @param planeNode x
	 * @param planeNode y
	 * @return int
	 */
	public int compare(node x, node y)
	{
		double xCost = x.getCosts() + getHeuristicCost(x.getState(), x.getCostsPerKm(), x.getCapacity(), x.getCity());
		double yCost = y.getCosts() + getHeuristicCost(y.getState(), y.getCostsPerKm(), y.getCapacity(), y.getCity());
		if(xCost < yCost) {
			return -1;
		} else if(xCost > yCost) {
			return 1;
		}
		return 0;
	}
	
	/**
	 * Heuristic function that computes a cost estimation until
	 * reaching the goal state. The heuristic function iterates on the
	 * remaining states and picks in each step the cheapest state transition until
	 * the goal state is reached.
	 * 
	 * @param taskList
	 * @param costPerKm
	 * @param capacity
	 * @param currentCity
	 * @return double
	 */
	
	public static double getHeuristicCost(ArrayList<ArrayList<Object>> taskList, int costPerKm, int capacity, City currentCity) {
		double heuristicCost = 0;
		
		ArrayList<ArrayList<Object>> states = new ArrayList<ArrayList<Object>>();
		
		for(int i=0; i < taskList.size(); i++){
			states.add(new ArrayList<Object>());
			states.get(i).add(taskList.get(i).get(0));
			states.get(i).add(taskList.get(i).get(1));			
		}
		
		int minAt = 0;
		double distance = 0;
		actionStates minStatus = null;
		Task bestTask = null;
		
		while(!AuctionTemplate.checkGoalState(states)) {
			double min = 100000;
			for(int i = 0; i < states.size(); i++) {
				if((actionStates) states.get(i).get(1) == actionStates.DELIVERED) {
					continue;
				}
				
				Task currentTask = (Task) states.get(i).get(0);
				actionStates currentTaskStatus = (actionStates) states.get(i).get(1);
				int currentTaskWeight = currentTask.weight;
				
				if(currentTaskStatus == actionStates.INITSTATE && currentTaskWeight < capacity) {
					distance = (double) currentCity.distanceTo(currentTask.pickupCity);
					if(distance < min) {
						min = distance;
						minAt = i;
						minStatus = actionStates.INITSTATE;
					}
				} else if(currentTaskStatus == actionStates.PICKEDUP) {
					distance = (double) currentCity.distanceTo(currentTask.deliveryCity);
					if(distance < min) {
						min = distance;
						minAt = i;
						minStatus = actionStates.PICKEDUP;
					}
				}
			}
			
			// Adjust the parameters depending on the state transition
			switch(minStatus) {
			case INITSTATE:
				states.get(minAt).set(1, actionStates.PICKEDUP);
				bestTask = (Task) states.get(minAt).get(0);
				currentCity = bestTask.pickupCity;
				capacity -= bestTask.weight;
				break;
			case PICKEDUP:
				states.get(minAt).set(1, actionStates.DELIVERED);
				bestTask = (Task) states.get(minAt).get(0);
				currentCity = bestTask.deliveryCity;
				capacity += bestTask.weight;
				break;
			default:
				break;
			}
			
			heuristicCost += distance * costPerKm;
		}
		
		return heuristicCost;
	}
}
