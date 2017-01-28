package soko;

public class Move {
	public int movedFrom;
	public int movedTo;
	public int boxFrom;
	public int boxTo;

	

	//This is actually the previous move because this list needs to be reversed to be in chronological order
    public Move next;
    
    
    public Move ( int from, int to, int bFrom, int bTo, Move pointer ) {
        movedFrom=from;
        movedTo=to;
        boxFrom=bFrom;
        boxTo=bTo;
        next=pointer;

      }
    
    
	/**
     * Copy constructor
     *
     * @param m Move
     */
    public Move (Move m) {
    	movedFrom = m.movedFrom;
    	movedTo = m.movedTo;
    	boxFrom=m.boxFrom;
    	boxTo=m.boxTo;
        next = null; 
      }
  
}
