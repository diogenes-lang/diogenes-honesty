package it.unica.co2.semantics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LTS {

	private Random random = new Random();
	private LTSState currentState;

	private Set<LTSState> alreadyVisitedStates = new HashSet<>();
	private List<LTSState> path = new ArrayList<>();
	
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
			System.out.println("\tstate hash: "+ currentState.hashCode());
			
			if (alreadyVisitedStates.contains(currentState)) {
				System.out.println("\tstate already visited, stop");
				break;
			}

			alreadyVisitedStates.add(currentState);
			path.add(currentState);

			LTSTransition[] nextTransitions = currentState.getAvailableTransitions();
			System.out.println("\tnumber of next states: "+ nextTransitions.length);
			
			int choice = random.nextInt(nextTransitions.length);
			currentState = nextTransitions[choice].apply();
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
			throw new LTSPropertyViolatedException(currentState, path);
		}
	}
}
