package soko;


import java.util.Arrays;
//import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Board {
	
	/** Used for debugging purposes.  Make true to print out more stuff for debugging */
	public boolean debug=false; 
	
	/** specifies which type of search initialized this board, 1 for forward board, 2 for backward board */
	public int startedBy;
	
	/** The width of the board. For easier access this variable is public. */
	public int width;

	/** The height of the board. For easier access this variable is public. */
	public int height;
	
	/** Constant for {@link #playerPosition} indicating "No player in level". */
	private final int NO_PLAYER = -1;	
	
	/** Player position, initialized with an "illegal" value.	*/
	private int playerPosition = NO_PLAYER;
	
	/**used for reverse search--holds the starting position for the guy from the original board, so the reverse search will know how to connect to the original board position.*/
	public int forwardStartingPosition;
	
	/**The board*/
	public char[] boardAsString;
	
	/**Goals and walls don't move, so make them an array tied to position on board*/
	public boolean[] goalsArray;
	
	/**Goals and walls don't move, so make them an array tied to position on board*/
	public boolean[] wallsArray;
	
	/**since all boxes are the same, I will just track where all of them are with a boxArray that is true or false for each position*/
	public boolean[] boxArray;
		
	/**
	 * Number of boxes in a level.
	 * This value always refers to the original number of boxes.
	 * For some calculations boxes are temporarily removed from the board,
	 * but that doesn't change this value.
	 */
	public int boxCount;

	/** Number of goals in a level.	*/
	public int goalsCount;
	
	/** the list of moves taken to get to this board configuration;
	 * 
	 *  move_list.next is a pointer to the next move in the move_list*/
	public Move move_list; 
	
	
	public Board() {
		//when a Board is first initialized, set the move_list to null
		move_list=null;
	}
	
	
	/**
     * Copy constructor
     *
     * @param b Board
     */
    public Board(Board b) {

    	// Create the board and use appropriate way to copy all parts of the original board
    	startedBy=b.startedBy;
    	goalsArray=b.goalsArray.clone();
    	wallsArray=b.wallsArray.clone();
    	boxArray=b.boxArray.clone();
    	height=b.height;
    	width=b.width;
    	playerPosition=b.playerPosition;
    	boardAsString=b.boardAsString.clone();
    	forwardStartingPosition=b.forwardStartingPosition;
    	//copying board so I can make a move on the copied board#####
    	
    	// Copy the moves that got us here
    	if ( b.move_list != null ) {
      	  Move ptr, b_ptr;
      	  Move temp = new Move(b.move_list);
      	  ptr = move_list = temp;
      	  b_ptr = b.move_list;
      	  while ( b_ptr.next != null ){
      	    b_ptr = b_ptr.next;
      	    temp = new Move(b_ptr);
      	    ptr.next = temp;
      	    ptr = temp;
      	  }
      	}
    	
    	// Need to capture everything because after a move
    	// the board will no longer be the same.
    }
	
	

	/** Prepare the board for the new level (also sets "width" and "height").
	 * 
	 * @param theBoardAsString String
	 * @param whoInitialized int
	 * 
	 * */
	public void setBoardFromString(String theBoardAsString, int whoInitialized){

		startedBy=whoInitialized; //specifies which type of search initialized this board, 1 for forward board, 2 for backward board

		// split string representing the board into rows
		String[] boardRows = theBoardAsString.split("\r\n");
		
		//remove backwards search starting position from board data (if it is there) so it doesn't make the wrong dimensions
		boardRows[boardRows.length-1]=boardRows[boardRows.length-1].replaceAll("\\d","");
		
		// Determine width and height
		for (String row : boardRows) {
			if (width < row.length()) {
				width = row.length();		// collect maximum
			}
		}
		height = boardRows.length;

		// Setup other aspects of the board--box, wall, goal, and player positions
		
		// Character in the level at a specific position (e.g. "#", "$", ...)
		int squareCharacter = 0;

		//Set goalsArray and wallsArray and boxArray to False everywhere so I can then just fill in where they are true
		goalsArray = new boolean[width*height];
		wallsArray = new boolean[width*height];
		boxArray = new boolean[width*height];
		
		// Set the elements of the new board.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				// Get the relevant board element.
				String row = boardRows[y];
				squareCharacter = row.charAt(x);
				
				switch ((char)squareCharacter) {
				case ' ':
					break;

				case '.':
					setGoal(x, y);
					break;

				case '$':
					setBox(x, y);
					break;

				case '*':
					setBox(x, y);
					setGoal(x, y);
					break;

				case '@':
					setPlayerPosition(x, y);
					break;

				case '+':
					setGoal(x, y);
					setPlayerPosition(x, y);
					break;

				case '#':
					setWall(x, y);
					break;

				default:
					// Invalid character in board file
					System.out.println( "Unknown square value in board data: "
							          + squareCharacter );
				}
			}
			System.out.println();
		}
		//fill the board with the correct values for the initial configuration
		boardToString();
		
		//print out board
		System.out.println("Forward Configuration");
		for (int x = 0; x < boardAsString.length; x=x+width) {
			System.out.println(Arrays.copyOfRange(boardAsString,x,x+width));
		}
		
		//print out player position as number
		System.out.println();
		System.out.println("Player starting position forward configuration: " + playerPosition);
		
	}
	
	
	/** Prepare the board for the new level (also sets "width" and "height"), but swap boxes and goals from the input file so you have a reverse board.
	 *   Use this when a reverse board is desired.
	 *   
	 * @param theBoardAsString String
	 * @param whoInitialized int
	 * 
	 * */
public void setBoardFromStringReverse(String theBoardAsString, int whoInitialized){
		
		startedBy=whoInitialized; //specifies which type of search initialized this board, 1 for forward board, 2 for backward board
		
		// split string representing the board into rows
		String[] boardRows = theBoardAsString.split("\r\n");
		
		//get starting position for reverse board by reading off number at end of board in text file
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(boardRows[boardRows.length-1]);
		while (m.find()) {
			setPlayerPosition(Integer.parseInt(m.group()));
		}
		
		//remove backwards search starting position from board data (if it is there) so it doesn't make the wrong dimensions
		boardRows[boardRows.length-1]=boardRows[boardRows.length-1].replaceAll("\\d","");
		
		// Determine width and height
		for (String row : boardRows) {
			if (width < row.length()) {
				width = row.length();		// collect maximum
			}
		}
		height = boardRows.length;

		// Setup other aspects of the board--box, wall, goal, and player positions
		
		// Character in the level at a specific position (e.g. "#", "$", ...)
		int squareCharacter = 0; 
		
		//Set goalsArray and wallsArray and boxArray to False everywhere so I can then just fill in where they are true
		goalsArray = new boolean[width*height];
		wallsArray = new boolean[width*height];
		boxArray = new boolean[width*height];
		
		// Set the elements of the new board.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				// Get the relevant board element.				
				String row = boardRows[y];
				squareCharacter = row.charAt(x);

				switch ((char)squareCharacter) {
				case ' ':
					break;

				case '$':
					setGoal(x, y);
					break;

				case '.':
					setBox(x, y);
					break;

				case '*':
					setBox(x, y);
					setGoal(x, y);
					break;

				case '@':
					//set the forward starting position, actually set the location of the player previously
					forwardStartingPosition = x + width * y;
					break;

				case '+':
					setBox(x, y);
					forwardStartingPosition = x + width * y;
					break;

				case '#':
					setWall(x, y);
					break;

				default:
					// Invalid character in board file
					System.out.println( "Unknown square value in board data: "
							          + squareCharacter );
				}
			}
		}
		
		//fill the board with the correct values for the initial configuration
		boardToString();
		
		//print out reverse board
		System.out.println();
		System.out.println("Reverse Configuration");
		printBoard();
		System.out.println("Player starting position reverse configuration: " + playerPosition);
		System.out.println();
		
	}
	
	
	
	
	/** Way to update a string array that captures the entire board state.  Overwrites boardAsString every time this method is called.
	 *   
	 * 
	 * 
	 * */
	public void boardToString(){

		// Create string array to store board state in 
		boardAsString = new char[wallsArray.length];
		
		
		for (int x=0; x<wallsArray.length; x++){
			if(goalsArray[x]==true){
				if (playerPosition==x){
					boardAsString[x]='+';
				}
				else if(boxArray[x]==true){
					boardAsString[x]='*';
				}
				else{
					boardAsString[x]='.';
				}
			}
			else if(boxArray[x]==true){
				boardAsString[x]='$';
			}
			else if(wallsArray[x]==true){
				boardAsString[x]='#';
			}
			else if(playerPosition==x){
				boardAsString[x]='@';
			}
			else{
				boardAsString[x]=' ';
			}
		}

	}
	
	
	/** Generate all possible moves for the player on the forward board and return as a move object (list of all possible moves for the player in the current position).
	 *   
	 * @return result Move
	 * 
	 * */	
	public Move genMovesForward(){  //should return a move
		//placeholder for checking if a box is clear on the other side for a push
		int positionToPushBoxInto=-1;
		
		//linked list of all possible moves
	    Move result = null;
	    
	    //these signify that there is a box to the cardinal direction of the player and the box is clear on other side and can be pushed--this will be checked later to see if it is not clear
		boolean[] boxDirectionClearDirection={true,true,true,true}; // (N,S,E,W)
				
		
		//get the number of the board position for each adjacent space to the player
		int[] adjacentSpaces=getAdjacentSpaces(playerPosition); //returns adjacent spaces in N,S,E,W format
				
		//determine what is in each of the adjacent spaces
		for (int y = 0; y < adjacentSpaces.length; y++) {
			//set positionToPushBoxInto and boxFrom to invalid at beginning of each loop
			positionToPushBoxInto=-1;
			int boxFrom=-1;
			
			//check for walls
			if(wallsArray[adjacentSpaces[y]]){
				adjacentSpaces[y]=-1; //a wall is adjacent in the direction of y (N,S,E,W)--assign to an invalid value
			}
			//check for boxes
			else if(boxArray[adjacentSpaces[y]]){
				//need to see what is on the other side of the box to see if we can push it
				positionToPushBoxInto=getAdjacentSpaces(adjacentSpaces[y])[y];
				boxDirectionClearDirection[y]=!wallsArray[positionToPushBoxInto];//if there is a wall to the Direction, then this will become false
				if (boxDirectionClearDirection[y]==true){ //if no walls to the beyond the box, see if there is another box beyond the box--if so, make this false
					boxDirectionClearDirection[y]=!boxArray[positionToPushBoxInto];
				}
												
				//whatever the value of box_X_clear_X_, it is correct at this point--if false, then there is something blocking the way of the box being pushed in that direction.  If true, then the box is clear to be pushed in that direction.
							
			}
			if (boxDirectionClearDirection[y]==false){
				adjacentSpaces[y]=-1;//set to invalid setting since we can't move in that direction
			}
			
			//any non-negative space in adjacentSpaces is clear at this point
			
			if(adjacentSpaces[y]!=-1){
				
				//Is the move a push?
				if(boxArray[adjacentSpaces[y]]==true){
					//we are pushing a box
					//set boxFrom to the space the box is in
					boxFrom=adjacentSpaces[y];
					//boxTo is the positionToPushBoxInto
				}
				else{
					//we are not pushing a box
					positionToPushBoxInto=-1; //there is a box on the other side of the box or there is not a box in that direction
					boxFrom=-1;
				}
				
				Move m = new Move(playerPosition, adjacentSpaces[y], boxFrom, positionToPushBoxInto, result );
	            //result is the linked list of all the moves
				result = m;
			}
		}
		//Returns moveList, a linked list of possible moves
	
		return result;
	}
	
	/** Generate all possible moves for the player on the reverse board and return as a move object (list of all possible moves for the player in the current position).
	 *   
	 * @return result Move
	 * 
	 * */
	public Move genMovesReverse(){  //should return a move
				
		//linked list of all possible moves
	    Move result = null;
	    
	    //print out current position if debugging
	    if (debug){
		    System.out.println("Current Position");
		    printBoard();
		    System.out.println("	Possible Moves");
	    }
	    
		//get the number of the board position for each adjacent space to the player
		int[] adjacentSpaces=getAdjacentSpaces(playerPosition); //returns adjacent spaces in N,S,E,W format
		
		//determine what is in each of the adjacent spaces
		for (int y = 0; y < adjacentSpaces.length; y++) {
					
			//set boxFrom to invalid at beginning of each loop
			int boxFrom=-1;
			
			//check for walls
			if(wallsArray[adjacentSpaces[y]]){
				adjacentSpaces[y]=-1; //a wall is adjacent in the direction of y (N,S,E,W)--assign to an invalid value
			}
			//check for boxes
			else if(boxArray[adjacentSpaces[y]]){
				
				//add possible box pull moves
				
				int positionAcrossFromBox=-1; //this will be the position adjacent to the player across from the sensed box; set to invalid each time first encountered
				
				//check if there is box or wall in the opposite direction of the box
				//get position adjacent to the player, but across from the box
				switch (y) {
					case 0: //North
						positionAcrossFromBox=adjacentSpaces[1];
						break;
	
					case 1: //South
						positionAcrossFromBox=adjacentSpaces[0];
						break;
	
					case 2: //East
						positionAcrossFromBox=adjacentSpaces[3];
						break;
	
					case 3: //West
						positionAcrossFromBox=adjacentSpaces[2];
						break;
				}
				
				//check if there is box or wall in the opposite direction of the box
				if (positionAcrossFromBox!=-1){//if it equals -1, then we already know there is a box or wall across from the box of interest--adjacentSpaces[y] was previously set to -1 and then the case statement assigned it to positionAcrossFromBox
					if(wallsArray[positionAcrossFromBox]==false & boxArray[positionAcrossFromBox]==false){
						//if a wall or a box are not in the position across from the box, add move to pull box there
						boxFrom=adjacentSpaces[y];
						Move m = new Move(playerPosition, positionAcrossFromBox, boxFrom, playerPosition, result );
			            //result is the linked list of all the moves
						result = m;
						if (debug){
							System.out.println("	Move Box "+oppositeDirection(y));
						}
					}	
				}
				//set a move in the direction of the box as invalid
				adjacentSpaces[y]=-1; //a box is adjacent in the direction of y (N,S,E,W)--assign to an invalid value because we cannot push it
			}
			
			
			//any non-negative space in adjacentSpaces is clear at this point
			if(adjacentSpaces[y]!=-1){
				//We are not dealing with box moves here, they were already dealt with above.  Set box stuff to invalid (-1) and make player only moves.
				Move m = new Move(playerPosition, adjacentSpaces[y], -1, -1, result );
	            //result is the linked list of all the moves
				result = m;
				if (debug){
					System.out.println("	Move Player "+direction(y));
				}
			}
		}
		//Return result, a linked list of possible moves	
		return result;
	}
	
	/** Provides direction in N,S,E,W for a given integer. 0=North, 1=South, 2=East, 3=West.
	 *   
	 *   
	 * @return cardinalDirection String
	 * 
	 * */	
	public String direction(int whichDirection){
		switch (whichDirection) {
		case 0: //North
			return "North";

		case 1: //South
			return "South";

		case 2: //East
			return "East";

		case 3: //West
			return "West";
		default:
			return "Invalid";
		}
	}
	
	
	/** Provides opposite direction in N,S,E,W for a given integer based on convention of 0=North, 1=South, 2=East, 3=West.  Therefore, returns 0=South, 1=North, 2=West, 3=East.
	 *   
	 *   
	 * @return oppositeCardinalDirection String
	 * 
	 * */	
	public String oppositeDirection(int whichDirection){
		switch (whichDirection) {
		case 1: //North
			return "North";

		case 0: //South
			return "South";

		case 3: //East
			return "East";

		case 2: //West
			return "West";
		default:
			return "Invalid";
		}
	}
	
	
	/** Makes the move passed in.  Typically used after a board has been copied.
	 *   
	 * 
	 * 
	 * */	
	public void makeMove(Move m){
		//all moves in m.movedTo should be adjacent to current position until after I move
		//move player
		playerPosition=m.movedTo;
		//move box if specified
		if (m.boxFrom!=-1){
			boxArray[m.boxFrom]=false;
			boxArray[m.boxTo]=true;
		}
		//update links in move_list
		 Move temp = new Move(m);
		 temp.next = move_list;
		 move_list = temp;
		 //update board state
		 boardToString();
		 if (debug){
			 printBoard();
		 }
	}
	
	
	/** Prints the board so you can see it.*/	
	public void printBoard(){
		for (int x = 0; x < boardAsString.length; x=x+width) {
			System.out.println(Arrays.copyOfRange(boardAsString,x,x+width));
		}
		System.out.println();
	}
	
	
	/** Gets spaces that are adjacent to the player on the board and returns the integer board position of each adjacent space in an adjacent spaces array {N,S,E,W}.
	 *      
	 * @return adjacentSpacesArray int[]
	 * 
	 * */
	public int[] getAdjacentSpaces(int position){
		//person will always be bounded by the walls, so no need to check bounds on the N, S, E, or W spaces
		int[] xy=getXY(position);
		int E = getBoardPosition(xy[0]+1, xy[1]);
		int W = getBoardPosition(xy[0]-1, xy[1]);
		int N = getBoardPosition(xy[0], xy[1]-1);
		int S = getBoardPosition(xy[0], xy[1]+1);
		int[] adjacentSpacesArray={N,S,E,W};
		return adjacentSpacesArray;
	}

	
	/** Gets the integer board position corresponding to passed in (x,y) board position.*/	
	public int getBoardPosition(int x, int y){
		return (x + width * y);
	}
	
	
	/** Gets the integer player position*/	
	public int getPlayerPosition(){
		return playerPosition;
	}

	
	/** Sets the player position by using the integer board position rather than (x,y)*/	
	public void setPlayerPosition(int position) {
		playerPosition = position;
	}

	/**
	 * Sets the player to the passed position.
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param x the x-coordinate of the position the player is to be set at.
	 * @param y the y-coordinate of the position the player is to be set at.
	 */
	public void setPlayerPosition(int x, int y) {
		playerPosition = x + width * y;
	}
	
	
	/**
	 * Sets or updates a particular element of the goal array as TRUE.
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param x the x-coordinate of the goal array to be set to TRUE.
	 * @param y the y-coordinate of the goal array to be set to TRUE.
	 */
	public void setGoal(int x, int y) {
		goalsArray[x + width * y] = true;
	}
	
	
	/**
	 * Sets or updates a particular element of the wall array as TRUE.
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param x the x-coordinate of the wall array to be set to TRUE.
	 * @param y the y-coordinate of the wall array to be set to TRUE.
	 */
	public void setWall(int x, int y) {
		wallsArray[x + width * y] = true;
	}
	
	
	/**
	 * Sets or updates a particular element of the box array as TRUE.
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param x the x-coordinate of the box array to be set to TRUE.
	 * @param y the y-coordinate of the box array to be set to TRUE.
	 */
	public void setBox(int x, int y) {
		boxArray[x + width * y] = true;
	}
	
	
	/**
	 * Sets or updates a particular element of the box array as TRUE by using the integer board position rather than (x,y).
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param position int board position rather than x,y
	 */
	public void setBox(int position) {
		boxArray[position] = true;
	}
	
	
	/**
	 * Pass in integer board position, get back (x,y) board position.
	 * The arguments are not checked against the board dimensions.
	 *
	 * @param position int board position rather than x,y
	 * @return xy (x,y) coordinates of passed in integer board position
	 */
	public int[] getXY(int position){
		int x = position % width;
		int y = position/width;
		int[] xy = {x,y};
		return xy; 
	}

	
	
	
}
