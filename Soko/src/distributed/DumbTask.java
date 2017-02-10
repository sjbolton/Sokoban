package distributed;

import java.util.concurrent.TimeUnit;

public class DumbTask extends Task {

	@Override
	public void run() {
		// Just print some text for about a minute
		boolean done = false;
		while (!done) {
			if (messageWaiting()) {
				done = (boolean) getMessage();
			}
			
			System.out.println("Don't question the system.");
			try {
				TimeUnit.SECONDS.sleep(1);
			} 
			catch (InterruptedException e) {}
		}
		System.out.println("End this madness, throw off your shackles and rise up!");
		returnObject = "Fight the power";
	}

}
