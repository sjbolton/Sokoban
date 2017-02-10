package distributed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import distributed.TestTask;

/**
 * This class is the basic worker for the distributed architecture.
 * It can be run on any machine, and need only be provided the address
 * of the JobManager. It will continue running after tasks are complete,
 * and will then ultimately be available to accept more tasks from the
 * JobManager.
 * 
 * @author 2dLt Ian McQuaid
 */
public class Worker {
	// Connectivity and streams
	private Socket control;
	private PrintWriter controlOut;
	private BufferedReader controlIn;
	private Socket data;
	private ObjectOutputStream dataOut;
	private ObjectInputStream dataIn;
	
	// Task execution logic
	private Task workerTask;
	private State workerState;
	
	// Valid states for the worker
	public enum State {
	    NO_TASK, NOT_STARTED, RUNNING
	}
	
	/**
	 * Basic constructor
	 */
	public Worker() {
		workerState = State.NO_TASK;
	}
	
	/**
	 * Connect to the server
	 * 
	 * @param serverLocation the IP address of the JobManager
	 * @return true if a connection was made, false otherwise
	 */
	public boolean connect(String serverLocation) {
		boolean successful = true;
		try
		{
			// Make the control connection
			control = new Socket("localhost", 9090);
			controlOut = new PrintWriter(control.getOutputStream(), true);
			controlIn = new BufferedReader(new InputStreamReader(control.getInputStream()));
			
			// Let the manager know your address
			controlOut.println(InetAddress.getLocalHost().getHostAddress());
			
			// Listen for the data connection part of the handshake
			ServerSocket listener = new ServerSocket(0);
			int portNum = listener.getLocalPort();
			controlOut.println(portNum);
			
			data = listener.accept();
			listener.close();
			
			// Set up the data connection streams.
			dataOut = new ObjectOutputStream(data.getOutputStream());
			dataIn = new ObjectInputStream(data.getInputStream());
		}
		catch (Exception excep)
		{
			successful = false;
		}
		return successful;
	}
	
	/**
	 * The main work loop of the worker. This will run endlessly for the
	 * life of the worker, executing task after task provided the worker
	 * isn't terminated in the interim.
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void work() throws Exception{
		// Infinite loop of performing work - kinda like life
		while(true) {
			String response;
			switch (workerState) {
				case NO_TASK:
					// Request a task
					controlOut.println("NO TASK");
					response = controlIn.readLine();
					if (response.equals("SENDING TASK")) {
						workerTask = (Task) dataIn.readObject();
						controlOut.println("TASK RECEIVED");
						controlIn.readLine(); // "TASK TRANSFERRED"
						workerState = State.NOT_STARTED;
					}
					break;
				case NOT_STARTED:
					// Ask to start
					controlOut.println("READY TO START");
					response = controlIn.readLine();
					if (response.equals("START")) {
						workerTask.start();
						workerState = State.RUNNING;
					}
					break;
				case RUNNING:
					if (workerTask.hasUniMessageToSend()) {
						// Does the task have anything to send?
						controlOut.println("MESSAGE TO SEND");
						
						// Pull from the task
						Object[] returnArray = workerTask.pullMessageFromUni();
						Serializable message = (Serializable) returnArray[0];
						int destination = (int) returnArray[1];
						
						// Write the message
						dataOut.writeObject(message);
						
						response = controlIn.readLine();
						if (response.equals("SEND DESTINATION")) {
							controlOut.println(destination);
						}
						
						response = controlIn.readLine(); // "MESSAGE TRANSFERRED"
					}
					else if (workerTask.hasMultiMessageToSend()) {
						// Does the task have anything to broadcast?
						controlOut.println("MESSAGE TO BROADCAST");
						
						// Pull from the task
						Serializable message = workerTask.pullMessageFromMulti();
						
						// Write the message
						dataOut.writeObject(message);
						
						response = controlIn.readLine(); // "BROADCAST COMPLETE"
					}
					else if (workerTask.isAlive()) {
						// Nothing, so see if the server has anything for us.
						controlOut.println("RUNNING");
						response = controlIn.readLine();
						if (response.equals("MESSAGE TO SEND")) {
							Serializable message = (Serializable) dataIn.readObject();
							workerTask.addMessageToMailbox(message);
							controlOut.println("MESSAGE RECEIVED");
							response = controlIn.readLine(); // "MESSAGE TRANSFERRED"
						}
					}
					else {
						// Finish-up work (i.e. return results)
						controlOut.println("TASK COMPLETE");
						dataOut.writeObject(workerTask.returnObject);
						controlIn.readLine(); // "OBJECT TRANSFERRED"
						
						// Clear the worker so it is ready for a new task
						workerTask = null;
						workerState = State.NO_TASK;
					}
					break;
				default:
					throw(new Exception("Invalid worker state."));
			}		
		}
	}
	
	/**
	 * This method disconnects the worker from the server. It
	 * is used as clean-up in case something causes the worker
	 * to crash.
	 * 
	 * @throws IOException if something goes wrong.
	 */
	public void disconnect() throws IOException {
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
	
	/**
	 * Start and execute the worker.
	 * 
	 * @param args optional argument for the server location. If not given, assumes localhost
	 * @throws Exception if something goes wrong
	 */
	public static void main(String[] args) throws Exception{
		System.out.println("Worker starting...");
		Worker worker = new Worker();
		String serverAddress = "localhost";
		
		// Use the command line server address, if it was provided
		if (args.length > 0) {
			serverAddress = args[0];
		}
		
		// Execute the worker
		try {
			// Connect
			boolean connected = false;
			System.out.println("Trying to connect to: " + serverAddress);
			while (!connected) {
				connected = worker.connect(serverAddress);
				TimeUnit.SECONDS.sleep(1);
			}
			System.out.println("Successfully connected");
			
			// Enter work loop
			worker.work();
		}
		finally {
			worker.disconnect();
		}
		System.out.print("Worker done. Exiting...");
	}
}
