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
	private ArrayList<ArrayList<Object>> biddingHistory= new ArrayList<ArrayList<Object>>();
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
			return computeIncoming(offer);

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
	private double adjustUsingFuture(double offer) {
		double adjustedOffer = offer;

		ArrayList<City> cityPath = createCityPath();

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
	public void addBiddingHistory(float ourBid, float opponentBid){
		ArrayList<Object> bids= new ArrayList<Object>();
		bids.add(ourBid);					//0 is our bid
		bids.add(opponentBid);				//1 is opponent bid
		biddingHistory.add(bids);
		computeHistoryTracking();
	}
	//computing index in biddingHistory with same behavior and computing if i m getting closer to the bids of the others (either winning or loosing)
	public void computeHistoryTracking(){
		int i= biddingHistory.size()-1;
		boolean won= false;
		int historyIndex= 0;
		if((Float)biddingHistory.get(i).get(0)< (Float)biddingHistory.get(i).get(1)) won= true;		//result of last auction 
		boolean foundChanging= false;
		while(i>=0 && foundChanging==false){
			if(won==true && (Float)biddingHistory.get(i).get(0)> (Float)biddingHistory.get(i).get(1)){
				foundChanging= true;
				if(biddingHistory.size()>1) historyIndex= i+1;
			}
			else if(won==false && (Float)biddingHistory.get(i).get(0)< (Float)biddingHistory.get(i).get(1)){
				foundChanging= true;
				if(biddingHistory.size()>1) historyIndex= i+1;
			}
			i--;
		}
		//extract last auctions with same result from biddingHistory (if any)
		ArrayList<ArrayList<Float>> bidSegment= new ArrayList<ArrayList<Float>>();
		for(int j= historyIndex; j< biddingHistory.size(); j++){
			ArrayList<Float> tmp= new ArrayList<Float>();
			tmp.add((Float)biddingHistory.get(j).get(0));
			tmp.add((Float)biddingHistory.get(j).get(1));
			bidSegment.add(tmp);
		}
		ArrayList<ArrayList<Float>> normalizedBidChunk= normalizeBids(bidSegment);
		System.out.println(normalizedBidChunk);
		float sum= 0;
		float average= 0;
		for(int j= 0; j< normalizedBidChunk.size(); j++){
			if(won) sum += (Float)normalizedBidChunk.get(j).get(0);
			else sum += (Float)normalizedBidChunk.get(j).get(1);
		}
		average= sum/(normalizedBidChunk.size());
		//the value of behavior in case of winning is positive if our bid is getting closer to the bid of the others, negative if we are getting farther 
		//the value of behavior in case of loosing is positive if our bid is getting closer to the bid of the others, negative if we are getting farther (no chance to win)
		int []behav;
		behav= new int[2];
		for(int j= 0; j< normalizedBidChunk.size(); j++){
			if(won){
				if(j<= normalizedBidChunk.size()/2){
					if((Float)normalizedBidChunk.get(j).get(0)-average<= 0)	behav[0]++;
					else if((Float)normalizedBidChunk.get(j).get(0)-average> 0) behav[0]--;
				}
				else if(j> normalizedBidChunk.size()/2){
					if((Float)normalizedBidChunk.get(j).get(0)-average<= 0)	behav[1]++;
					else if((Float)normalizedBidChunk.get(j).get(0)-average> 0) behav[1]--;
				}
			}
			else if(!won){
				if(j<= normalizedBidChunk.size()/2){
					if((Float)normalizedBidChunk.get(j).get(1)-average<= 0)	behav[0]++;
					else if((Float)normalizedBidChunk.get(j).get(1)-average> 0) behav[0]--;
				}
				else if(j> normalizedBidChunk.size()/2){
					if((Float)normalizedBidChunk.get(j).get(1)-average<= 0)	behav[1]++;
					else if((Float)normalizedBidChunk.get(j).get(1)-average> 0) behav[1]--;
				}
			}
		}
		boolean behavior = false;		//false means i m getting further 
		if(behav[0]>=0 && behav[0]>=behav[1])	behavior= true;
		else if(behav[0]<0 && behav[0]<behav[1])	behavior= false;
		System.out.println(">>>>>>"+ behavior);

		biddingHistory.get(biddingHistory.size()-1).add(won);																			//memorize the result (index 2)
		biddingHistory.get(biddingHistory.size()-1).add(behavior);																		//memorize the behavior	(index 3)
		//means i keep having the same behavior than before (getting closer or getting farther)
		if(biddingHistory.size()>1 && (((Boolean)biddingHistory.get(biddingHistory.size()-2).get(3)==true && behavior==true)||((Boolean)biddingHistory.get(biddingHistory.size()-2).get(3)==false && behavior==false))){
			timeSinceLastChange++;
		}
		//means i changed behavior (getting closer or getting farther)
		else {
			timeSinceLastChange= 0;			
		}
	}
	public ArrayList<ArrayList<Float>> normalizeBids(ArrayList<ArrayList<Float>> bidSegment){
		ArrayList<ArrayList<Float>> normalizeSegmentBids= new ArrayList<ArrayList<Float>>();
		boolean won= false;
		if(bidSegment.get(bidSegment.size()-1).get(1)>bidSegment.get(bidSegment.size()-1).get(0))	won= true;
		for(int i=0; i< bidSegment.size(); i++){
			ArrayList<Float> normalizedBids= new ArrayList<Float>();
			if(won==true){		//normalizing wrt opponent bids (we won)		(opponent bids are 1 and ours are between 0 and 1)
				normalizedBids.add(bidSegment.get(i).get(0)/bidSegment.get(i).get(1));
				normalizedBids.add(bidSegment.get(i).get(1)/bidSegment.get(i).get(1));
			}
			else{			//normalizing wrt our bids (we lost)			(our bids are 1 and opponent bids are between 0 and 1)
				normalizedBids.add(bidSegment.get(i).get(0)/bidSegment.get(i).get(0));
				normalizedBids.add(bidSegment.get(i).get(1)/bidSegment.get(i).get(0));
			}
			normalizeSegmentBids.add(normalizedBids);
		}
		return normalizeSegmentBids;
	}
	public double computeIncoming(double bid){
		//case we won 
		if(biddingHistory.size()>1 && (Boolean)biddingHistory.get(biddingHistory.size()-1).get(2)==true){
			//we are getting closer to the bid of the others	the function used: http://www.wolframalpha.com/input/?i=plot+%28x-4%29%2F%282*sqrt%281%2B%28x-4%29^2%29%29+%2B0.5
			if((Boolean)biddingHistory.get(biddingHistory.size()-1).get(3)==true){
				double value= (timeSinceLastChange-4)/(2*Math.sqrt(1+ Math.pow((double)(timeSinceLastChange-4),2.0)))	 +.5;
				//actualIncoming is in the range MINIMUM_INCOMING MAXIMUM_INCOMING according to the value of the function
				actualIncoming= MINIMUM_INCOMING + (MAXIMUM_INCOMING*value);
				System.out.println("case we won and we are getting closer to the other bids. actualIncoming: "+ (1+actualIncoming));
				//returns the bid incremented of the wanted percentage
				return bid*(1+actualIncoming);
			}
			//we are getting further from the bid of the others	the function used : http://www.wolframalpha.com/input/?i=plot+exp%28x%29%2F20
			else if((Boolean)biddingHistory.get(biddingHistory.size()-1).get(3)==false){
				double value= Math.exp(timeSinceLastChange)/20;
				//actualIncoming is in the range MINIMUM_INCOMING MAXIMUM_INCOMING according to the value of the function
				actualIncoming= MINIMUM_INCOMING+ (MAXIMUM_INCOMING*Math.min(value, 1.0));
				System.out.println("case we won and we are getting further from the other bids. actualIncoming: "+ (1+actualIncoming));
				//returns the bid incremented of the wanted percentage
				return bid*(1+actualIncoming);
			}
		}
		//case we lost
		else if(biddingHistory.size()>1 && (Boolean)biddingHistory.get(biddingHistory.size()-1).get(2)==false){
			//we are getting closer to the bids of the others	 function used: http://www.wolframalpha.com/input/?i=plot+-%281%2F5%29x%2B1
			if((Boolean)biddingHistory.get(biddingHistory.size()-1).get(3)==true){
				double value= -(1/5)*timeSinceLastChange +1;
				//actualIncoming is between the range of previous actualIncoming and the MINIMUM_INCOMING according to the value returned by the function
				actualIncoming= MINIMUM_INCOMING + actualIncoming*value;
				System.out.println("case we lost and we are getting closer to the other bids. actualIncoming: "+ actualIncoming);
				//returns a bid which contains a smaller reward compared to the previous auction
				return bid*(1+actualIncoming);
			}
			//we are getting further from the bids of the others(low chance to win, the others are more efficient). function used: http://www.wolframalpha.com/input/?i=plot+-exp%28x%29%2F20%2B+1
			else if((Boolean)biddingHistory.get(biddingHistory.size()-1).get(3)==false){
				System.out.println("---->THEY ARE BEING EFFICIENT!!!!!!!!");
				double value= -Math.exp(timeSinceLastChange)/20 +1;
				actualIncoming= MINIMUM_INCOMING + actualIncoming*value;
				System.out.println("case we lost and we are getting further from the other bids. actualIncoming: "+ actualIncoming);
				//returns a bid which contains a smaller reward compared to the previous auction
				return bid*(1+actualIncoming);
			}
		}
		else{
			return bid* (1+actualIncoming);
		}

		return bid;
	}

}
