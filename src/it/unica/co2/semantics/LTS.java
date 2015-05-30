package it.unica.co2.semantics;

import java.util.Random;

public class LTS {

	private Random random = new Random();
	private LTSState currentState;

	public LTS(LTSState startState) {
		this.currentState = startState;
	}
	
	public LTSState start() {
		return start(LTSState.class);
	}
	
	public <T extends LTSState> T start(Class<T> clazz) {
		
		int iterations=0;
		
		System.out.println("-- START --");
		
		checkState();
		
		while (currentState.hasNext()) {
			iterations++;
			System.out.println("iter: "+ iterations);
			
			System.out.println("\tcurrentState: "+ currentState);

			LTSState[] nextStates = currentState.nextStates();
			System.out.println("\tnumber of next states: "+ nextStates.length);
			
			int choice = random.nextInt(nextStates.length);
			currentState = nextStates[choice];
			System.out.println("\tchoosed state: "+ currentState);
			
			checkState();
		}
		
		System.out.println("iterations: "+iterations);
		System.out.println("-- FINISH --");
		return clazz.cast(currentState);
	}

	private void checkState() {
		
		if (!currentState.check()) {
			System.out.println("\tthe state violate the property, throwing an exception");
			throw new LTSPropertyViolatedException(currentState);
		}
	}
}
