package distributed;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Override this class and provide your own custom run() function
 * in order to push work to the JobManager. For communication, use
 * the provided methods, as they cover the necessary synchronization
 * between the task thread and the worker (communication) thread. 
 * 
 * For the task override-er, the messageWaiting(), getMessage(),
 * sendMessage(), and broadcastMessage() functions are the ones
 * needed to perform whatever communication your tasks will require.
 * 
 * For the task user (read: the worker), you need everything else. And
 * you need to not screw up synchronization. And you need to do it
 * efficiently enough that the Task doesn't just fail. Best of luck!
 * 
 * @author 2dLt Ian McQuaid
 */
public abstract class Task extends Thread implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Message buffers. Please use accessors to manipulate these.
	// Otherwise, synchronization gets weird, things break, and
	// the world itself begins to descend into a pit of hellfire.
	private LinkedList<Serializable> inBuffer;
	private LinkedList<Serializable> outMultiBuffer;
	private LinkedList<Serializable> outUniBuffer;
	private LinkedList<Integer> destBuffer;
	
	// This object is what will be returned to the manager when
	// the thread finally dies.
	public Object returnObject;
	
	/**
	 *  Basic constructor
	 */
	public Task() {
		inBuffer = new LinkedList<Serializable>();
		outMultiBuffer = new LinkedList<Serializable>();
		outUniBuffer = new LinkedList<Serializable>();
		destBuffer = new LinkedList<Integer>();
		returnObject = null;
	}
	
	// The user must implement this method
	public abstract void run();
	
	/**
	 * Check if a message has arrived for the thread.
	 * 
	 * @return true if a message is waiting for this thread
	 */
	public boolean messageWaiting() {
		boolean hasMessage;
		synchronized(inBuffer) {
			hasMessage = !inBuffer.isEmpty();
		}
		return hasMessage;
	}
	
	/**
	 * Check if a message needs sent from the uni buffer.
	 * 
	 * @return true if a message is waiting
	 */
	public boolean hasUniMessageToSend() {
		boolean hasMessage;
		synchronized(outUniBuffer) {
			hasMessage = !outUniBuffer.isEmpty();
		}
		return hasMessage;
	}
	
	/**
	 * Check if a message needs sent from the multi buffer.
	 * 
	 * @return true if a message is waiting
	 */
	public boolean hasMultiMessageToSend() {
		boolean hasMessage;
		synchronized(outMultiBuffer) {
			hasMessage = !outMultiBuffer.isEmpty();
		}
		return hasMessage;
	}
	
	/**
	 * Get a single message on the queue for this thread.
	 * 
	 * @return the message
	 */
	public Serializable getMessage() {
		Serializable message;
		synchronized(inBuffer) {
			message = inBuffer.poll();
		}
		return message;
	}
	
	/**
	 * Send a message to a single task.
	 * 
	 * @param dest the index of the recipient task
	 * @param message the message to be sent
	 */
	public void sendMessage(int dest, Serializable message) {
		synchronized(outUniBuffer) {
			outUniBuffer.add(message);
			destBuffer.add(dest);
		}
	}
	
	/**
	 * Broadcast a message to every task (other than yourself)
	 * 
	 * @param message the message to be sent
	 */
	public void broadcastMessage(Serializable message) {
		synchronized(outMultiBuffer) {
			outMultiBuffer.add(message);
		}
	}
	
	/**
	 * Pushes a new message into the Tasks message-waiting buffer.
	 * 
	 * @param message the message to add
	 */
	public void addMessageToMailbox(Serializable message) {
		synchronized(inBuffer) {
			message = inBuffer.add(message);
		}
	}
	
	/**
	 * Grabs a message which is waiting to be transmitted from the
	 * tasks's uni buffer. Also grabs the destination.
	 * 
	 * @return an object array. The first element is the message,
	 * the second is the destination (integer).
	 */
	public Object[] pullMessageFromUni() {
		Object[] returnArray = new Object[2];
		synchronized(outUniBuffer) {
			returnArray[0] = outUniBuffer.poll();
			returnArray[1] = destBuffer.poll();
			
		}
		return returnArray;
	}
	
	/**
	 * Grabs a message which is waiting to be transmitted from the
	 * task's multi buffer.
	 * 
	 * @return the message
	 */
	public Serializable pullMessageFromMulti() {
		Serializable message;
		synchronized(outMultiBuffer) {
			message = outMultiBuffer.poll();
		}
		return message;
	}
}
