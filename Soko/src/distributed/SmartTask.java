package distributed;

import java.util.concurrent.TimeUnit;

public class SmartTask extends Task {
	
	private long timeToWait = 10000;

	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		// Just print some text for about a minute
		System.out.println("Wait for " + timeToWait/1000 + " seconds.");
		while (System.currentTimeMillis() - startTime < timeToWait) {
			System.out.println(((System.currentTimeMillis() - startTime)/1000) + "s: contemplating solutions.");
			try {
				TimeUnit.SECONDS.sleep(1);
			} 
			catch (InterruptedException e) {}
		}
		boolean shouldWeDoSomethingAboutThisInsanity = true;
		broadcastMessage(shouldWeDoSomethingAboutThisInsanity);
		System.out.println("Tell them all, the time for blind obedience is over.");
		returnObject = "Follow me to freedom.";
	}
}
