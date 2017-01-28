package soko;

import java.io.*;

import soko.Board;
import soko.Move;
import soko.Search;
import soko.Fwd_Bwd_BiDir_Search;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;


public class PlaySoko {
	//Shared objects
	public static Map<String, Board> theConcurrentHashMapObject = new ConcurrentHashMap<String, Board>(); //Shared Hashmap for bidirectional search 
	//public final static int THREAD_POOL_SIZE = 2;  //Placeholder for threading
	
	public static Move result;//list of moves from solution back to start
	public static Board board;//workspace
	public static Search search; // the search algorithm
	public static long startTime; //timer
	
	public static Board board2;//used for bidirectional search
	public static Move biDirectionalresult;
	
	//public static int threadNumber;//Placeholder for threading
	

	public PlaySoko() {
		
	}

	public static String newBoard[][];//array for boards
	
	
	
	/**
	 * main method: Runs the game.
	 * Specify the file to read in as boardString, and the searchType and then run
	 * 
	 * @param
	 * @return
	 */
	
	public static void main(String[] args) throws IOException {
		//determine what kind of search you want to perform
		//set searchType to:
		//	1 if forward Breadth First Search
		//	2 if backward Breadth First Search
		//	3 if bi-directional search
		int searchType=3;
				
		//read in file with sokoban board
		String boardString=readFile("SimpleBoard.txt");
		
		//setup the forward board
		board = new Board();
		board.setBoardFromString(boardString,1);
		
		//setup the reverse board
		board2 = new Board();
		board2.setBoardFromStringReverse(boardString, 2);
		
		//create a new search
		search = new Fwd_Bwd_BiDir_Search(board, board2, theConcurrentHashMapObject, searchType);
		startTime = System.currentTimeMillis();
		
		//run the search specified by searchType
		switch (searchType) {
		case 1: //forward breadth first search
			result = search.findMoves();
			break;

		case 2: //backwards breadth first search
			result = search.findMovesReverse();
			break;

		case 3: //bidirectional breadth first search
			result = search.findMovesBidirectional();
			break;
			
		default:
			System.out.println("Invalid Search Choice");
			return;
		}


		//print out timing and nodes visited during search
		System.out.println( "Board: " + ( (float)(System.currentTimeMillis() - startTime) / 1000.0) +
				" seconds" );
		System.out.println("Nodes Visited: " + search.nodeCount());

		// Before printing need to reverse the move list
		if ( result == null )
			System.out.println("No path found!");
		else {
			if (searchType!=2){//backwards search is already the correct orientation
			result = reverseMoves(result);
			}
			else{//correct to from order for reverse moves
				result=correctToFromReverse(result);
				result = reverseMoves(result);
			}
			//print out the moves taken to solve the board
			for (Move theMove = result; theMove != null; theMove = theMove.next ) {

				if (theMove.boxFrom!=-1){
					System.out.print("Move from " + theMove.movedFrom + " to " + theMove.movedTo + ".  ");
					System.out.println("Moved box from " + theMove.boxFrom + " to " + theMove.boxTo + ".");
				}
				else{
					System.out.println("Move from " + theMove.movedFrom + " to " + theMove.movedTo + ".");
				}     

			}
		}
		System.out.println();
		

	}

	
	
	/**
	 * correctToFromReverse method: switches to and from locations for the backward search move list.
	 * This is because during the backward search the moves are made in reverse
	 * compared to the forward search.  To get the correct orientation, the to and from need to be swapped.
	 * 
	 * @param b_m
	 * @return Move
	 */
	private static Move correctToFromReverse(Move b_m) {
		Move b_ptr, temp;
		Move extra;
		Move forward = new Move(b_m);
		b_ptr = b_m;
		
		while ( b_ptr.next != null ){
			temp = new Move(b_ptr);
			extra= new Move(b_ptr);
			b_ptr = b_ptr.next;
			forward.boxFrom=extra.boxTo;
			forward.boxTo=extra.boxFrom;
			forward.movedFrom=extra.movedTo;
			forward.movedTo=extra.movedFrom;
			temp.next = forward;
			forward = temp;
			if(b_ptr.next==null){
				//get the last one
				temp = new Move(b_ptr);
				extra= new Move(b_ptr);
				forward.boxFrom=extra.boxTo;
				forward.boxTo=extra.boxFrom;
				forward.movedFrom=extra.movedTo;
				forward.movedTo=extra.movedFrom;
				temp.next = forward;
				forward = temp;
			}
		}
		
		return forward.next;//accidentally added in another move during while loop--correct it here
		
	}


	/**
	 * reverseMoves method: reverses the ordering of a move list.
	 * This is because during the search the moves are stored in reverse order
	 * to make the addition of items less of a hassle.
	 * 
	 * @param b_m
	 * @return Move
	 */
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


	static String readFile(String path) 
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}
	
	
}
