package soko;
/**
 * <p>Title: Rush Hour</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: AFIT</p>
 * @author not attributable
 * @version 1.0
 */

interface Search {
		
    /**
     * The findMoves method will be where the search code actually goes. It is 
     * expecting a return of the move list to the goal. The Board objects keep
     * track of the moves which got you to the goal so all you need to do is once
     * the goal is found return the move_list for the goal Board.
     * 
     * @return
     */
    public Move findMoves();
    
    /**
     * Inside your search you should also keep an incrementor around which keeps
     * track of the number of nodes expanded. This method just returns that count.
     * 
     * @return
     */
    public long nodeCount();

	public Move findMovesReverse();

	public Move findMovesBidirectional();
	
}
