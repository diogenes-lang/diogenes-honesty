package it.unica.co2.semantics;

import java.util.Random;

public class LTS<T> {

	private Random random = new Random();
	private LTSState<T> currentState;

	public LTS(LTSState<T> startState) {
		this.currentState = startState;
	}
	
	
	public void start() {
		
		int iterations=0;
		
		System.out.println("-- START --");
		
		while (currentState.hasNext()) {
			iterations++;
			System.out.println("iter: "+ iterations);
			
			System.out.println("\tcurrentState: "+ currentState);
			LTSState<T>[] nextStates = currentState.nextStates();
			
			System.out.println("\tnumber of next states: "+ nextStates.length);
			
			int choice = random.nextInt(nextStates.length);
			
			currentState = nextStates[choice];
			System.out.println("\tchoosed state: "+ currentState);
		}
		
		System.out.println("iterations: "+iterations);
		System.out.println("-- FINISH --");
	}

}
