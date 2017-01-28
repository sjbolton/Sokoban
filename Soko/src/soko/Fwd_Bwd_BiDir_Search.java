package soko;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;


public class Fwd_Bwd_BiDir_Search implements Search {
	public double nodeRatio= 1; //set this for bidirectional searches.  Specifies the ratio of backward nodes to expand compared to forward nodes to expand.  
	
	boolean biDirectional=true; //set to false if you want to speed up unidirectional searches
	
	//forward search objects
	private Board node;
	private LinkedList<Board> frontier = new LinkedList<Board>(); //nodes that I am expanding
	private HashMap<String, Board> frontierHash = new HashMap<String, Board>(); //hashMap to maintain the frontier
	private HashMap<String, Board> explored = new HashMap<String, Board>(); //hashMap to make sure we don't expand nodes we have already explored
	private long nodeCount1=0; //used to track how many total nodes have been expanded
	private long nodeCount2=0; //used to track how many nodes forward search has expanded
	private long nodeCount3=0; //used to track how many nodes reverse search has expanded
	
	
	//backward search objects
	public Board nodeReverse;//used for bidirectional search
	private LinkedList<Board> frontierReverse = new LinkedList<Board>(); //nodes that I am expanding
	private HashMap<String, Board> frontierReverseHash = new HashMap<String, Board>(); //hashMap to maintain the frontier
	private HashMap<String, Board> exploredReverse = new HashMap<String, Board>(); //hashMap to make sure we don't expand nodes we have already explored
	
	//Shared objects
	public Map<String, Board> theConcurrentHashMapObject;// = new ConcurrentHashMap<String, Board>();
	int searchType=0;//will be set algorithmically (user does not need to set this here) to 1, 2, or 3 depending on what type of search to run (Forward BFS, Backward BFS, Bidirectional BFS).
	int nextSearchTurn=1;//used later to track whether forward or backward search is executing
	
	/**
	 * Creates a search object.  
	 * Need to specify nodeRatio for bidirectional searches.
	 * If doing bi-directional search, need to set the nodeRatio to an appropriate level.  As board complexity increases, set this to a higher value.  Typical values should go from .5 to 30.
	 * Because forward search is less efficient, to keep the number of nodes expanded in each direction the same (and significantly improve performance), only do forward search when it has expanded less nodes than reverse search--otherwise skip to reverse search
	 *
	 * @param board Forward direction board
	 * @param board2 Reverse direction board
	 * @param concurrentHashMap shared hashmap for bidirectional search, not used if unidirectional search
	 * @param theSearchType 1 for forward search, 2 for backward search, 3 for bidirectional search
	 */
	public Fwd_Bwd_BiDir_Search(Board board, Board board2, Map<String, Board> concurrentHashMap, int theSearchType) {
		theConcurrentHashMapObject = concurrentHashMap;
		node = board; //add board with no moves made yet as only element	
		nodeReverse = board2; //add a reverse board with no moves made yet as only element	
		searchType = theSearchType; 
	}

	
	
	@Override	
	
	/** Perform breadth first search to solve in the forward direction.
	 * 
	 * */
	public Move findMoves() {
		
				//perform forward breadth first search
				
				
				// uses enhanced breadth-first search to find the sequence of moves to solve sokoban forward
				
				////////////////////////////////////////////
				// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
				if (Arrays.equals(node.boxArray, node.goalsArray)){ //check to see if board was already in a solution configuration
					return node.move_list; //board was already in a solution configuration	
				}
						
				
				////////////////////////////////////////////
				//frontier <-- a FIFO queue with node as the only element
				frontier.add(node);
				frontierHash.put(Arrays.toString(node.boardAsString), node); //maintain hash table for lookup reference for later--frontier hash needs to hash the entire table because just hashing on box and goal positions is not unique
				
				////////////////////////////////
				//explored <-- an empty set
				// already created and empty
				
				/////////////////////////////////
				//loop do
				while(true){
					
					//////////////////////////
					//if EMPTY?(frontier) then return failure
					if(frontier.isEmpty()==true){
						return null;
					}
					
					//////////////////////////
					//node<--POP(frontier) /*chooses the shallowest node in frontier*/
					node=frontier.pop();
					frontierHash.remove(Arrays.toString(node.boardAsString)); //maintain current frontier hash table
					
					////////////////////////////
					//add node.STATE to explored
					explored.put(Arrays.toString(node.boardAsString), node);
					nodeCount1++;
					
					////////////////////////////
					//for each action in problem.ACTIONS(node.STATE) do
					Move theMoves = node.genMovesForward(); //gets each action
					
					
					while(theMoves!=null){
						///////////////////
						//child<--CHILD-NODE(problem, node, action)
						Board child= new Board(node);//copy the board since it is an object and I don't want to just have a reference
						child.makeMove(theMoves); //make the move to make the child a unique node 				
						
						///////////////////////
						//if child.STATE is not in explored or frontier then
						if (frontierHash.containsKey(Arrays.toString(child.boardAsString))==false && explored.containsKey(Arrays.toString(child.boardAsString))==false){
							
							
							/////////////////
							// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
							if (Arrays.equals(child.boxArray, child.goalsArray)){ //check to see if board is in a solution configuration
								return child.move_list; //return solution
							}
							
							//this part no longer necessary, but might be used in the future--leaving it for now
							else if(biDirectional){//check to see if a solution has been found with bi-directional search--still safe to use with single direction search
								Move possibleSolution=checkForIntersection(child); //check for an intersection between this search and the backwards search--if only performing single direction search, this is still safe and will run fine
								if (possibleSolution!=null){ 
									return possibleSolution; //a solution exists, if not, keep searching...
								}
							}
							
							
							
							/////////////////////
							//frontier<--INSERT(child,frontier)
							frontier.add(child); // add child to the frontier
							frontierHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierHash
			
						}
						
						// part of the for each action in problem.ACTIONS(node.STATE) do
						theMoves=theMoves.next;
					}
					
					
				}
					
			
		
		
		
		
	}
	
	
	@Override
	/** Perform breadth first search to solve in the reverse direction.
	 * 
	 * */
	public Move findMovesReverse() {
		// uses enhanced breadth-first search to find the sequence of moves to solve sokoban in reverse
		
		////////////////////////////////////////////
		// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
		if (Arrays.equals(nodeReverse.boxArray, nodeReverse.goalsArray)){ //check to see if board was already in a solution configuration
			return nodeReverse.move_list; //board was already in a solution configuration	
		}
				
		
		////////////////////////////////////////////
		//frontierReverse <-- a FIFO queue with nodeReverse as the only element
		frontierReverse.add(nodeReverse);
		frontierReverseHash.put(Arrays.toString(nodeReverse.boardAsString), nodeReverse); //maintain hash table for lookup reference for later--frontierReverse hash needs to hash the entire table because just hashing on box and goal positions is not unique
		
		////////////////////////////////
		//exploredReverse <-- an empty set
		// already created and empty
		
		/////////////////////////////////
		//loop do
		while(true){
			
			//////////////////////////
			//if EMPTY?(frontierReverse) then return failure
			if(frontierReverse.isEmpty()==true){
				return null;
			}
			
			//////////////////////////
			//nodeReverse<--POP(frontierReverse) /*chooses the shallowest nodeReverse in frontierReverse*/
			nodeReverse=frontierReverse.pop();
			frontierReverseHash.remove(Arrays.toString(nodeReverse.boardAsString)); //maintain current frontierReverse hash table
			
			////////////////////////////
			//add nodeReverse.STATE to exploredReverse
			exploredReverse.put(Arrays.toString(nodeReverse.boardAsString), nodeReverse);
			nodeCount1++;
			
			////////////////////////////
			//for each action in problem.ACTIONS(nodeReverse.STATE) do
			Move theMoves = nodeReverse.genMovesReverse(); //gets each action
			
			
			while(theMoves!=null){
				///////////////////
				//child<--CHILD-nodeReverse(problem, nodeReverse, action)
				Board child= new Board(nodeReverse);//copy the board since it is an object and I don't want to just have a reference
				child.makeMove(theMoves); //make the move to make the child a unique nodeReverse 				
				
				///////////////////////
				//if child.STATE is not in exploredReverse or frontierReverse then
				if (frontierReverseHash.containsKey(Arrays.toString(child.boardAsString))==false && exploredReverse.containsKey(Arrays.toString(child.boardAsString))==false){
					
					
					/////////////////
					// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
					if (Arrays.equals(child.boxArray, child.goalsArray)){ //check to see if board is in a solution configuration
						Move finalMoveList=canPlayerMoveToSolutionPosition(child, 1);
						return finalMoveList; //return solution
					}
					
					//this part no longer necessary, but might be used in the future--leaving it for now
					else if(biDirectional){//check to see if a solution has been found with bi-directional search--still safe to use with single direction search
						Move possibleSolution=checkForIntersection(child); //check for an intersection between this search and the backwards search--if only performing single direction search, this is still safe and will run fine
						if (possibleSolution!=null){ 
							return possibleSolution; //a solution exists, if not, keep searching...
						}
					}
					
					/////////////////////
					//frontierReverse<--INSERT(child,frontierReverse)
					frontierReverse.add(child); // add child to the frontierReverse
					frontierReverseHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierReverseHash

				}
				
				// part of the for each action in problem.ACTIONS(nodeReverse.STATE) do
				theMoves=theMoves.next;
			}
			
			
		}
		
		
	}
	
	
	/**Counts the total number of nodes expanded. */
	@Override
	public long nodeCount() {
		// returns the number of nodes explored prior to finding the solution
		return nodeCount1;
	}
	
	/** Checks shared hashmap (concurrentHashMapObject) for intersection with child (board passed in).
	 * 
	 * @param child current board configuration
	 *  
	 * @return the connecting moves to get from one board state with the same box locations to another board state with the same box locations or null if solution not found.
	 * */
	private Move checkForIntersection(Board child) {
		// Checks for intersection of forward and backward search
		if (theConcurrentHashMapObject.containsKey(Arrays.toString(child.boxArray))){
			if (theConcurrentHashMapObject.get(Arrays.toString(child.boxArray)).startedBy!=child.startedBy){
				//check boards quickly to make sure the boxes are in the same positions
				if (Arrays.equals(theConcurrentHashMapObject.get(Arrays.toString(child.boxArray)).boxArray, child.boxArray)){ //Make sure it was not a collision on the hash table--if the two box arrays are equal, then continue 
					//same box configuration, now check if person can move to the location of the person on the board without moving any boxes 
					return canPlayerMoveToSolutionPosition(child);
				}
				else{
					return null;
				}
			}
			else{
				return null;
			}
		}
		else{//not an intersection
			return null;
		}
	}
	
	
	/** Makes copy of the board and creates a new board where boxes are walls.  Then passes this new board to findConnectingMoves which uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions.  Used during goal checking.
	 * 
	 * @param child current board configuration
	 *  
	 * @return the connecting moves to get from one board state with the same box locations to another board state with the same box locations.
	 * */
	private Move canPlayerMoveToSolutionPosition(Board child){
		//make copy of board and create new board where boxes are walls
		//this can be used for both forward and backward searches
		
		Board childCopy = new Board(child);//copy the board since it is an object and I don't want to just have a reference
		
		//turn boxes into walls on newly copied board
		for (int x=0; x<childCopy.boxArray.length; x++){
			if (childCopy.boxArray[x]==true){
				childCopy.wallsArray[x]=true;
				childCopy.boxArray[x]=false;
			}
		}
		
		//perform search on new board to try to connect the player positions
		return findConnectingMoves(childCopy, Arrays.toString(child.boxArray)); //this returns the entire move list from the first move made in whatever direction search calls this to the last move required to connect the two spaces, or null if unable to connect the spaces
				
	}
	
	/** Makes copy of the board and creates a new board where boxes are walls.  Then passes this new board to findConnectingMoves which uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions.  Used during goal checking at the end of backward search.
	 * 
	 * @param child current board configuration
	 * @param finalPosition position you want to see if the player can reach without moving any boxes
	 * 
	 * @return the connecting moves to get from one board state with the same box locations to another board state with the same box locations.
	 * */
	private Move canPlayerMoveToSolutionPosition(Board child, int finalPosition){
		//make copy of board and create new board where boxes are walls
		//this can be used for both forward and backward searches
		
		Board childCopy = new Board(child);//copy the board since it is an object and I don't want to just have a reference
		
		//turn boxes into walls on newly copied board
		for (int x=0; x<childCopy.boxArray.length; x++){
			if (childCopy.boxArray[x]==true){
				childCopy.wallsArray[x]=true; //update walls array
				childCopy.boxArray[x]=false; //update box array
				childCopy.boardAsString[x]='#';//update boardAsString to keep consistent
			}
		}
		
		//perform search on new board to try to connect the player positions
		return findConnectingMoves(childCopy, finalPosition); //this returns the entire move list from the first move made in whatever direction search calls this to the last move required to connect the two spaces, or null if unable to connect the spaces
				
	}

	
	/** Uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions.  Used during goal checking in bi-directional search.
	 * 
	 * @param childCopy current board of possible goal configuration for the search that found possible goal configuration second.
	 * @param theHashKey hashkey which maps to the board that arrived at the possible goal configuration first.  
	 * 
	 * @return the connecting moves to get from one board state with the same box locations to another board state with the same box locations.
	 * */
	public Move findConnectingMoves(Board childCopy, String theHashKey) {
		// uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions
		// move player around in BFS fashion until he finds the other board's player's spot or return null
		// this can be used for both forward and backward searches
		
		
		// make the scope of some of the variables used in this search local
		LinkedList<Board> frontier = new LinkedList<Board>(); //nodes that I am expanding
		HashMap<String, Board> frontierHash = new HashMap<String, Board>(); 
		HashMap<String, Board> explored = new HashMap<String, Board>(); 
		
		
		// get theHashCode board
		int theHashKeyPlayerPosition=theConcurrentHashMapObject.get(theHashKey).getPlayerPosition();
		Board node= new Board(childCopy);//turn node into a local variable for this search
		
		
		////////////////////////////////////////////
		// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
		if (theHashKeyPlayerPosition==node.getPlayerPosition()){ //check to see if board was already in a solution configuration
			//solutionFoundIsSameLocation=1; //set this equal to 1 to show that we have a solution
			return node.move_list; //board was already in a solution configuration--move_list should contain all moves made up until this point in the forward or backward search			
		}
				
		
		////////////////////////////////////////////
		//frontier <-- a FIFO queue with node as the only element
		frontier.add(node);
		frontierHash.put(Arrays.toString(node.boardAsString), node); //maintain hash table for lookup reference for later--frontier hash needs to hash the entire table because just hashing on box and goal positions is not unique
		
		////////////////////////////////
		//explored <-- an empty set
		// already created and empty
		
		/////////////////////////////////
		//loop do
		while(true){
			
			//////////////////////////
			//if EMPTY?(frontier) then return failure
			if(frontier.isEmpty()==true){
				return null;
			}
			
			//////////////////////////
			//node<--POP(frontier) /*chooses the shallowest node in frontier*/
			node=frontier.pop();
			frontierHash.remove(Arrays.toString(node.boardAsString)); //maintain current frontier hash table
			
			////////////////////////////
			//add node.STATE to explored
			explored.put(Arrays.toString(node.boardAsString), node);
			//nodeCount1++;
			
			////////////////////////////
			//for each action in problem.ACTIONS(node.STATE) do
			Move theMoves = node.genMovesForward(); //gets each action
			
			
			while(theMoves!=null){
				///////////////////
				//child<--CHILD-NODE(problem, node, action)
				Board child= new Board(node);//copy the board since it is an object and I don't want to just have a reference
				child.makeMove(theMoves); //make the move to make the child a unique node 				
				
				///////////////////////
				//if child.STATE is not in explored or frontier then
				if (frontierHash.containsKey(Arrays.toString(child.boardAsString))==false && explored.containsKey(Arrays.toString(child.boardAsString))==false){
					
					
					/////////////////
					// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
					if (theHashKeyPlayerPosition==child.getPlayerPosition()){ //check to see if board is in a solution configuration
						return child.move_list; //return moves required to connect the two positions
					}
					
					/////////////////////
					//frontier<--INSERT(child,frontier)
					frontier.add(child); // add child to the frontier
					frontierHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierHash
	
				}
				
				// part of the for each action in problem.ACTIONS(node.STATE) do
				theMoves=theMoves.next;
			}
			
			
		}
		
		
	}
	
	/** Uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions.  Used during goal checking at the end of backward search.
	 * 
	 * @param childCopy current board configuration
	 * @param finalPosition position you want to see if the player can reach without moving any boxes
	 * 
	 * @return the connecting moves to get from one board state with the same box locations to another board state with the same box locations.
	 * */
	public Move findConnectingMoves(Board childCopy, int finalPosition) {
		// uses enhanced breadth-first search to find the sequence of moves to connect the two player's positions
		// move player around in BFS fashion until he finds the other board's player's spot or return null
		// this can be used for both forward and backward searches
		
		
		// make the scope of some of the variables used in this search local
		LinkedList<Board> frontier = new LinkedList<Board>(); //nodes that I am expanding
		HashMap<String, Board> frontierHash = new HashMap<String, Board>(); 
		HashMap<String, Board> explored = new HashMap<String, Board>(); 
		
		
		// get theHashCode board--this is where I want to go to
		int theForwardStartingPosition=childCopy.forwardStartingPosition;
		Board node= new Board(childCopy);//turn node into a local variable for this search
		
		//node.printBoard(); //used for debugging
		
		////////////////////////////////////////////
		// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
		if (theForwardStartingPosition==node.getPlayerPosition()){ //check to see if board was already in a solution configuration
			//solutionFoundIsSameLocation=1; //set this equal to 1 to show that we have a solution
			return node.move_list; //board was already in a solution configuration--move_list should contain all moves made up until this point in the forward or backward search			
		}
				
		
		////////////////////////////////////////////
		//frontier <-- a FIFO queue with node as the only element
		frontier.add(node);
		frontierHash.put(Arrays.toString(node.boardAsString), node); //maintain hash table for lookup reference for later--frontier hash needs to hash the entire table because just hashing on box and goal positions is not unique
		
		////////////////////////////////
		//explored <-- an empty set
		// already created and empty
		
		/////////////////////////////////
		//loop do
		while(true){
			
			//////////////////////////
			//if EMPTY?(frontier) then return failure
			if(frontier.isEmpty()==true){
				return null;
			}
			
			//////////////////////////
			//node<--POP(frontier) /*chooses the shallowest node in frontier*/
			node=frontier.pop();
			frontierHash.remove(Arrays.toString(node.boardAsString)); //maintain current frontier hash table
			
			////////////////////////////
			//add node.STATE to explored
			explored.put(Arrays.toString(node.boardAsString), node);
			nodeCount1++;
			
			////////////////////////////
			//for each action in problem.ACTIONS(node.STATE) do
			Move theMoves = node.genMovesForward(); //gets each action
			
			
			while(theMoves!=null){
				///////////////////
				//child<--CHILD-NODE(problem, node, action)
				Board child= new Board(node);//copy the board since it is an object and I don't want to just have a reference
				child.makeMove(theMoves); //make the move to make the child a unique node 				
				
				///////////////////////
				//if child.STATE is not in explored or frontier then
				if (frontierHash.containsKey(Arrays.toString(child.boardAsString))==false && explored.containsKey(Arrays.toString(child.boardAsString))==false){
					
					
					/////////////////
					// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
					if (theForwardStartingPosition==child.getPlayerPosition()){ //check to see if board is in a solution configuration
						return child.move_list; //return moves required to connect the two positions
					}
					
					/////////////////////
					//frontier<--INSERT(child,frontier)
					frontier.add(child); // add child to the frontier
					frontierHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierHash
	
				}
				
				// part of the for each action in problem.ACTIONS(node.STATE) do
				theMoves=theMoves.next;
			}
			
			
		}
		
		
	}
	
	


	@Override
	/** Perform bidirectional search using breadth first search in both directions to solve Sokoban board.
	 * 
	 * */
	public Move findMovesBidirectional(){
		//create nodeCount2 and nodeCount3 and make the backwards search continue node expansion until it has expanded the same number of nodes as the forward search times the nodeRatio and then let the forward search go again

		//performs one node expansion using forward, then one backwards and keeps flipping back and forth until a solution is found if nodeRatio=1

		Move forwardMoves;//used for stitching together forward and backward searches
		Move reverseMoves;//used for stitching together forward and backward searches
		Move theMoves;
		
		// Setup forward and backward searches

		////////////////////////////////////////////
		// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
		if (Arrays.equals(node.boxArray, node.goalsArray)){ //check to see if board was already in a solution configuration
			System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
			return node.move_list; //board was already in a solution configuration	
		}

		////////////////////////////////////////////
		// if problem.GOAL-TEST(node.STATE) then return SOLUTION(node)
		if (Arrays.equals(nodeReverse.boxArray, nodeReverse.goalsArray)){ //check to see if board was already in a solution configuration
			System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
			return nodeReverse.move_list; //board was already in a solution configuration	
		}



		////////////////////////////////////////////
		//frontier <-- a FIFO queue with node as the only element
		frontier.add(node);
		frontierHash.put(Arrays.toString(node.boardAsString), node); //maintain hash table for lookup reference for later--frontier hash needs to hash the entire table because just hashing on box and goal positions is not unique
		theConcurrentHashMapObject.put(Arrays.toString(node.boxArray), node);//add node to theConcurrentHashMapObject	

		////////////////////////////////////////////
		//frontierReverse <-- a FIFO queue with nodeReverse as the only element
		frontierReverse.add(nodeReverse);
		frontierReverseHash.put(Arrays.toString(nodeReverse.boardAsString), nodeReverse); //maintain hash table for lookup reference for later--frontierReverse hash needs to hash the entire table because just hashing on box and goal positions is not unique
		theConcurrentHashMapObject.put(Arrays.toString(nodeReverse.boxArray), nodeReverse);//add node to theConcurrentHashMapObject		


		////////////////////////////////
		//explored <-- an empty set
		// already created and empty

		/////////////////////////////////
		//loop do
		//
		
		while(true){
			
			//this is where the switching between forward and backward search happens
			if (nodeRatio*nodeCount2<=nodeCount3){// expand forward search nodes
				//because forward search is less efficient, to keep the number of nodes expanded in each direction at the specified node ratio (and significantly improve performance), only do forward search when it has expanded less nodes than nodeRatio times the reverse search--otherwise skip to reverse search
				
				//////////////////////////
				//if EMPTY?(frontier) then return failure because we completely explored the space in one direction
				if(frontier.isEmpty()==true){
					System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
					return null;
				}

				//////////////////////////
				//node<--POP(frontier) /*chooses the shallowest node in frontier*/
				node=frontier.pop();
				frontierHash.remove(Arrays.toString(node.boardAsString)); //maintain current frontier hash table



				////////////////////////////
				//add node.STATE to explored
				explored.put(Arrays.toString(node.boardAsString), node);
				nodeCount1++;
				nodeCount2++;//track how many nodes forward search has expanded


				////////////////////////////
				//for each action in problem.ACTIONS(node.STATE) do
				theMoves = node.genMovesForward(); //gets each action


				while(theMoves!=null){
					///////////////////
					//child<--CHILD-NODE(problem, node, action)
					Board child= new Board(node);//copy the board since it is an object and I don't want to just have a reference
					child.makeMove(theMoves); //make the move to make the child a unique node 				

					///////////////////////
					//if child.STATE is not in explored or frontier then
					if (frontierHash.containsKey(Arrays.toString(child.boardAsString))==false && explored.containsKey(Arrays.toString(child.boardAsString))==false){


						/////////////////
						// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
						if (Arrays.equals(child.boxArray, child.goalsArray)){ //check to see if board is in a solution configuration
							System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
							return child.move_list; //return solution
						}
						else if(biDirectional){//check to see if a solution has been found with bi-directional search--still safe to use with single direction search
							Move possibleSolution=checkForIntersection(child); //check for an intersection between this search and the backwards search--if only performing single direction search, this is still safe and will run fine
							if (possibleSolution!=null){ 
								forwardMoves=possibleSolution; //these are the forward moves
								reverseMoves=theConcurrentHashMapObject.get(Arrays.toString(child.boxArray)).move_list;//these are the backwards moves--need to move one move back because it saved it's move from this spot not going to it
								Move finalSolution=stitchTogetherMoves(forwardMoves,reverseMoves);
								System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
								return finalSolution; //a solution exists, if not, keep searching...
							}
						}

						/////////////////////
						//frontier<--INSERT(child,frontier)
						frontier.add(child); // add child to the frontier
						frontierHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierHash
						theConcurrentHashMapObject.put(Arrays.toString(child.boxArray), child);//add node to theConcurrentHashMapObject

					}

					// part of the for each action in problem.ACTIONS(node.STATE) do
					theMoves=theMoves.next;
				}
			}

			else{// expand reverse search nodes
				
				
				//////////////////////////
				//if EMPTY?(frontierReverse) then return failure because we completely explored the space in one direction
				if(frontierReverse.isEmpty()==true){
					System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
					return null;
				}


				//////////////////////////
				//nodeReverse<--POP(frontierReverse) /*chooses the shallowest nodeReverse in frontierReverse*/
				nodeReverse=frontierReverse.pop();
				frontierReverseHash.remove(Arrays.toString(nodeReverse.boardAsString)); //maintain current frontierReverse hash table


				////////////////////////////
				//add nodeReverse.STATE to exploredReverse
				exploredReverse.put(Arrays.toString(nodeReverse.boardAsString), nodeReverse);
				nodeCount1++;
				nodeCount3++;//track how many nodes reverse search has expanded

				//if(nodeCount1>10000 & nodeCount1<10003){
				//	node.printBoard();
				//	nodeReverse.printBoard();
				//}


				//perform reverse search

				////////////////////////////
				//for each action in problem.ACTIONS(nodeReverse.STATE) do
				theMoves = nodeReverse.genMovesReverse(); //gets each action


				while(theMoves!=null){
					///////////////////
					//child<--CHILD-nodeReverse(problem, nodeReverse, action)
					Board child= new Board(nodeReverse);//copy the board since it is an object and I don't want to just have a reference
					child.makeMove(theMoves); //make the move to make the child a unique nodeReverse 				

					///////////////////////
					//if child.STATE is not in exploredReverse or frontierReverse then
					if (frontierReverseHash.containsKey(Arrays.toString(child.boardAsString))==false && exploredReverse.containsKey(Arrays.toString(child.boardAsString))==false){


						/////////////////
						// if problem.GOAL-TEST(child.STATE) then return SOLUTION(child)
						if (Arrays.equals(child.boxArray, child.goalsArray)){ //check to see if board is in a solution configuration
							System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
							return child.move_list; //return solution
						}
						else if(biDirectional){//check to see if a solution has been found with bi-directional search--still safe to use with single direction search
							Move possibleSolution=checkForIntersection(child); //check for an intersection between this search and the backwards search--if only performing single direction search, this is still safe and will run fine
							if (possibleSolution!=null){ 
								forwardMoves=theConcurrentHashMapObject.get(Arrays.toString(child.boxArray)).move_list;//these are the backwards moves--need to move one move back because it saved it's move from this spot not going to it
								reverseMoves=possibleSolution; //these are the backwards moves
								Move finalSolution=stitchTogetherMoves(forwardMoves,reverseMoves);
								System.out.println("Forward Search Expanded " + nodeCount2+" nodes.  Reverse search expanded "+nodeCount3+" nodes.");
								return finalSolution;
							}
						}

						/////////////////////
						//frontierReverse<--INSERT(child,frontierReverse)
						frontierReverse.add(child); // add child to the frontierReverse
						frontierReverseHash.put(Arrays.toString(child.boardAsString), child); //maintain frontierReverseHash
						theConcurrentHashMapObject.put(Arrays.toString(child.boxArray), child);//add node to theConcurrentHashMapObject

					}

					// part of the for each action in problem.ACTIONS(nodeReverse.STATE) do
					theMoves=theMoves.next;
				}
			}


		}


	}

	/** Reverses the reverse moves and appends to the forward moves.
	 * 
	 * @return forwardMoves; final list of all the moves oriented in the forwardMoves direction
	 * */
	private Move stitchTogetherMoves(Move forwardMoves, Move reverseMoves) {
		//need to reverse the reverse moves and append to the forward moves
		
		//check to see if reverseMoves found the connecting point
		if (reverseMoves.movedTo==forwardMoves.movedFrom){
			forwardMoves=forwardMoves.next;//remove one forward move
		}
		//check to see if forwardMoves found the connecting point
		else if (forwardMoves.movedTo==reverseMoves.movedFrom){
			reverseMoves=reverseMoves.next;//remove one reverse move
		}
		
		//flip around the reverse moves
		
		while(reverseMoves.next!=null){
			Move temp = new Move(reverseMoves);
			Move temp2= new Move(reverseMoves);
			//switch orientation of moves and box pushes
			temp.boxFrom=temp2.boxTo;
			temp.boxTo=temp2.boxFrom;
			temp.movedFrom=temp2.movedTo;
			temp.movedTo=temp2.movedFrom;
			//set pointer to point to the correct next move
			temp.next = forwardMoves;
			forwardMoves=temp;
			reverseMoves=reverseMoves.next;
			if(reverseMoves.next==null){
				//get the last one
				temp = new Move(reverseMoves);
				temp2 = new Move(reverseMoves);
				temp.boxFrom=temp2.boxTo;
				temp.boxTo=temp2.boxFrom;
				temp.movedFrom=temp2.movedTo;
				temp.movedTo=temp2.movedFrom;
				temp.next = forwardMoves;
				forwardMoves = temp;
			}
		}
		

		return forwardMoves;


	}
	
	
	/** Reverses the move list from the backward search that was passed in
	 * 
	 * @return forward; returns forward oriented moves of passed in reverse moves list
	 * */
	private static Move reverseMoves(Move b_m) {
		Move b_ptr, temp;

		Move forward = new Move(b_m);
		b_ptr = b_m;
		while ( b_ptr.next != null ){
			b_ptr = b_ptr.next;
			temp = new Move(b_ptr);
			temp.next = forward;
			forward = temp;
		}
		return forward;
	}  
	

	
	
	
	
	
	
	
	

}
