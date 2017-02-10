package distributed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * This is the server for the distributed architecture. It 
 * handles the connections to all of the other workers, as 
 * well as coordinates the messages by organizing said worker
 * threads.
 * 
 * It shares a communication protocol with the worker, which
 * allows it to pass objects back and forth. The objects contained
 * are all thread-safe if they are accessed via their associated
 * geters and seters. Direct access is, thus, highly discouraged.
 * 
 * All of that to say, that it works real good. Be happy, for our
 * cup truly overfloweth with code-y goodness.
 * 
 * @author 2dLt Ian McQuaid
 */
public class JobManager {
	
	// Manager data members
	private LinkedList<Task> taskQueue;
	private LinkedList<Integer> indexQueue;
	private HashMap<Integer, WorkerThread> threadMap;
	private HashMap<Integer, Object> resultsMap;
	
	// Status of the current job
	private boolean jobStarted;
	private int taskCount;
	private int finishCount;
	
	/**
	 * Basic constructor
	 * 
	 * @throws IOException is things break
	 */
	public JobManager () throws IOException {
		taskQueue = new LinkedList<Task>();
		indexQueue = new LinkedList<Integer>();
		threadMap = new HashMap<Integer, WorkerThread>();
		resultsMap = new HashMap<Integer, Object>();
		jobStarted = false;
		taskCount = 0;
		finishCount = 0;
		
		// Start listening for connections.
		ConnectionThread thread = new ConnectionThread(this);
		thread.start();
	}
	
	/**
	 * Add a task to the manager.
	 * 
	 * @param newTask the task to be added
	 * @param taskIndex the index of the task, in the eyes of the user. This is the
	 * same as the destination that the task specifies when sending a message.
	 */
	public void addTask(Task newTask, int taskIndex) {
		synchronized(taskQueue) {
			taskQueue.add(newTask);
			indexQueue.add(taskIndex);
			taskCount++;
		}
	}
	
	/**
	 * Retrieve a task and index for a given thread. Also marks
	 * the thread as owning said task.
	 * 
	 * @param thread the thread who will be running this task
	 * @return a two element array containing the task and the task index, respectively.
	 */
	public Object[] getTask(WorkerThread thread) {
		synchronized(taskQueue) {
			Object[] returnObj = new Object[2];
			if (!taskQueue.isEmpty()) {
				// Mark this in the thread map
				int index = indexQueue.poll();
				this.addToThreadmap(index, thread);
				returnObj[0] = taskQueue.poll();
				returnObj[1] = index;
				return returnObj;
			}
			else { // taskQueue.isEmpty()
				return null;
			}
		}
	}
	
	/**
	 * Places this destination-thread pair into the threadmap.
	 * 
	 * @param dest the destination
	 * @param thread the thread
	 */
	private void addToThreadmap(int dest, WorkerThread thread) {
		synchronized (threadMap) {
			threadMap.put(dest, thread);
		}
	}
	
	/**
	 * Retrieve a thread from the threadmap, given a destination index.
	 * 
	 * @param dest the destination index
	 * @return the thread owning said destination index
	 */
	public WorkerThread getFromThreadmap(int dest) {
		synchronized (threadMap) {
			return threadMap.get(dest);
		}
	}
	
	/**
	 * Deletes a destination entry from the threadmap.
	 * 
	 * @param dest the destination to delete
	 */
	public void removeFromThreadmap(int dest) {
		synchronized (threadMap) {
			threadMap.remove(dest);
		}
	}
	
	/**
	 * Get every value contained in the threadmap.
	 * 
	 * @return the threadmap values
	 */
	public Collection<WorkerThread> getAllFromThreadmap() {
		synchronized (threadMap) {
			return threadMap.values();
		}
	}
	
	/**
	 * Add an object to the result map.
	 * 
	 * @param index the task index
	 * @param returnObj the result object
	 */
	public void addToResultMap(int index, Object returnObj) {
		synchronized (resultsMap) {
			resultsMap.put(index, returnObj);
			finishCount++;
		}
	}
	
	/**
	 * Begin execution of the assigned tasks.
	 */
	public void startJob() {
		jobStarted = true;
	}
	
	/**
	 * Block until execution of the assigned tasks is complete.
	 */
	public void waitForJob() {
		if (jobStarted) {
			while (finishCount < taskCount) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} 
				catch (InterruptedException e) {}
			}
		}
	}
	
	/**
	 * This thread handles listening for incoming worker connections.
	 * 
	 * @author 2dLt Ian McQuaid
	 */
	private class ConnectionThread extends Thread {
		JobManager manager;
		
		/**
		 * Basic constructor for the thread.
		 * 
		 * @param jManager pointer to the manager
		 */
		public ConnectionThread(JobManager jManager) {
			manager = jManager;
		}
		
		/**
		 * Run the connection listener. Endlessly listens for any
		 * workers trying to connect.
		 */
		@Override
		public void run() {
			ServerSocket listener = null;
			try {
				listener = new ServerSocket(9090);
				while (true) {
					Socket newConn = listener.accept();
					
					// Make and start the thread for this connection. It handles itself from
					// here.
					WorkerThread thread = new WorkerThread(manager, newConn);
					thread.start();
				}
			}
			catch (Exception excep) {}
			finally {
				try {
					listener.close();
				}
				catch (Exception excep) {}
			}
		}
	}
	
	/**
	 * This thread handles an individual connection. It includes the connection
	 * protocol (read: magic), as well as appropriate sockets, streams, and buffers.
	 * It will try to pull tasks from the manager, if its worker needs one, will send
	 * the contents of its messageBuffer along to its worker (with no regard to how
	 * that message got there), and will pull objects back from its worker to push to
	 * other worker threads when its worker requests.
	 * 
	 * For those following along at home, the worker and server maintain a heartbeat
	 * connection with a THREAD_HEARTBEAT_DELAY second time interval between beats.
	 * This is defaulted to 1 second, but can be adjusted if necessary. The goal is
	 * to keep the two as consistent as is reasonably possible, without slowing down
	 * the network/runtime with pointless traffic. The traffic, otherwise, has a call
	 * and response feel to it, with the worker always speaking first and the server
	 * speaking last (followed by a forced delay by the server) on each beat.
	 * 
	 * @author 2dLt Ian McQuaid
	 */
	private class WorkerThread extends Thread {
		// Pointer to the manager
		private JobManager manager;
		
		// Messages to be sent to this worker
		private LinkedList<Serializable> messageBuffer;
		
		// Connectivity
		private Socket control;
		private BufferedReader controlIn;
		private PrintWriter controlOut;
		private Socket data;
		private ObjectInputStream dataIn;
		private ObjectOutputStream dataOut;
		
		// The index of the current task
		private int taskIndex;
		
		// Delay between beats, in seconds
		public static final long THREAD_HEARTBEAT_DELAY = 1;

		/**
		 * Basic constructor
		 * 
		 * @param mngr pointer to the job manager
		 * @param initialConnection the socket of the control connection
		 * @throws IOException if things go wrong
		 */
		public WorkerThread(JobManager mngr, Socket initialConnection) throws IOException {
			// Keep a pointer to the manager
			manager = mngr;
			taskIndex = -1;
			
			// Allocate memory for the message buffer
			messageBuffer = new LinkedList<>();
			
			// Set the control streams
			control = initialConnection;
			controlIn = new BufferedReader(new InputStreamReader(control.getInputStream()));
			controlOut = new PrintWriter(control.getOutputStream(), true);
			
			// Make the data connection
			String serverAddress = controlIn.readLine();
			int portNum = Integer.parseInt(controlIn.readLine());
			data = new Socket(serverAddress, portNum);
			
			// Set the data streams
			dataIn = new ObjectInputStream(data.getInputStream());
			dataOut = new ObjectOutputStream(data.getOutputStream());
		}
		
		/**
		 * Threads all have a run() function which contains their concurrent
		 * execution. In other words, this contains everything important.
		 */
		@Override
		public void run() {
			try {
				boolean workerConnected = true;
				while (workerConnected) {
					String message = controlIn.readLine();
					switch (message) {
						case "NO TASK":
							// Check and see if there is a job in the queue to assign
							Object[] returnObj = manager.getTask(this);							
							if (returnObj != null) {
								Task unassignedTask = (Task) returnObj[0];
								taskIndex = (int) returnObj[1];
								while (!message.equals("TASK RECEIVED")) {
									controlOut.println("SENDING TASK");
									dataOut.writeObject(unassignedTask);
									message = controlIn.readLine();
								}
								controlOut.println("TASK TRANSFERRED");
							}
							else { // returnObj == null
								controlOut.println("NONE AVAILABLE");
							}
							break;
						case "READY TO START":
							// Can this worker start yet?
							if (jobStarted) {
								controlOut.println("START");
							}
							else { // !jobStarted
								controlOut.println("WAIT");
							}
							break;
						case "RUNNING":
							// Nothing needs sent from the worker, so check if we
							// have anything to send them.
							Serializable dataMessage = this.getMessage();
							if (dataMessage != null) {
								controlOut.println("MESSAGE TO SEND");
								while (!message.equals("MESSAGE RECEIVED")) {
									dataOut.writeObject(dataMessage);
									message = controlIn.readLine();
								}
								controlOut.println("MESSAGE TRANSFERRED");
							}
							else { // dataMessage == null
								controlOut.println("CONTINUE");
							}
							break;
						case "MESSAGE TO SEND":
							// Receive the message
							Serializable incomingMessage = (Serializable) dataIn.readObject();
							controlOut.println("SEND DESTINATION");
							int dest = Integer.parseInt(controlIn.readLine());
							
							// Now use the thread map to plant this message in the
							// appropriate buffer
							manager.getFromThreadmap(dest).addMessage(incomingMessage);
							controlOut.println("MESSAGE TRANSFERRED");
							break;
						case "MESSAGE TO BROADCAST":
							// Receive the message
							Serializable incomingBroadcast = (Serializable) dataIn.readObject();
							
							// Now use the thread map to get every thread
							Iterator<WorkerThread> allThreads = manager.getAllFromThreadmap().iterator();
							while (allThreads.hasNext()) {
								WorkerThread currThread = allThreads.next();
								if (currThread != this) {
									currThread.addMessage(incomingBroadcast);
								}
							}
							
							controlOut.println("BROADCAST COMPLETE");
							break;
						case "TASK COMPLETE":
							Object taskObject = dataIn.readObject();
							controlOut.println("OBJECT TRANSFERRED");
							manager.addToResultMap(taskIndex, taskObject);
							break;
						default:
							System.out.println("Error. Invalid message received from worker: " + message);
							break;
					}
					
					// Wait a little bit (to keep network traffic to a minimum
					TimeUnit.SECONDS.sleep(THREAD_HEARTBEAT_DELAY);
				}
			}
			catch (Exception excep) {
				excep.printStackTrace();
			}
			finally {
				// The worker connection was lost, so go ahead and clean up
				// the thread before it goes out of scope.
				this.disconnect();
			}			
		}
		
		/**
		 * Push a message to this thread's buffer. It will (eventually) be sent
		 * to that worker.
		 * 
		 * @param message the message to be added/sent
		 */
		public void addMessage(Serializable message) {
			synchronized (messageBuffer) {
				messageBuffer.add(message);
			}
		}
		
		/**
		 * Retrieve a message from this thread's message buffer.
		 * 
		 * @return the message
		 */
		public Serializable getMessage() {
			synchronized (messageBuffer) {
				if (!messageBuffer.isEmpty()) {
					return messageBuffer.poll();
				}
				else { // messageBuffer.isEmpty()
					return null;
				}
			}
		}
		
		/**
		 * End the connection between this thread and the worker.
		 */
		public void disconnect() {
			try {
				if (control != null) {
					control.close();
				}
				if (controlOut != null) {
					controlOut.close();
				}
				if (controlIn != null) {
					controlIn.close();
				}
				if (data != null) {
					data.close();
				}
				if (dataOut != null) {
					dataOut.close();
				}
				if (dataIn != null) {
					dataIn.close();
				}
			}
			catch (IOException excep) {
				excep.printStackTrace();
			}
		}
	}
	
	/**
	 * Test method. Does nothing important, except when it does. Sometimes
	 * breaks, you never really know.
	 * 
	 * I take no responsibility for the things that do, or do not, happen
	 * because of this method being executed.
	 * 
	 * @param args not used
	 * @throws IOException presumably things broke
	 */
	public static void main(String[] args) throws IOException {
		JobManager jm = new JobManager();
		
		jm.addTask(new TestTask(), 1);
		//jm.addTask(new TestTask(), 2);
		//jm.addTask(new TestTask(), 3);
		
		//jm.addTask(new DumbTask(), 1);
		//jm.addTask(new DumbTask(), 2);
		//jm.addTask(new SmartTask(), 3);
		
		System.out.println("Starting job.");
		jm.startJob();
		jm.waitForJob();
		
		Iterator<Object> results = jm.resultsMap.values().iterator();
		System.out.println("Job complete");
		while (results.hasNext()) {
			System.out.println("Result = : " + results.next());
		}
	}
}
