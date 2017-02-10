package distributed;

import java.util.concurrent.TimeUnit;

public class TestTask extends Task {
	
	private long timeToWait = 10000;

	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		// Just print some text for about a minute
		System.out.println("Wait for " + timeToWait/1000 + " seconds.");
		while (System.currentTimeMillis() - startTime < timeToWait) {
			System.out.println(((System.currentTimeMillis() - startTime)/1000) + "s: WHAT AN AWESOME TASK!!!");
			try {
				TimeUnit.SECONDS.sleep(1);
			} 
			catch (InterruptedException e) {}
		}
		
		returnObject = "A pretty cool string";
	}

}
