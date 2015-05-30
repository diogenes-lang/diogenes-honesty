package it.unica.co2.model.contract;

import java.util.Arrays;
import java.util.stream.Collectors;


public class InternalSum extends Contract{

	private final InternalAction[] actions;

	public InternalSum(InternalAction... actions) {
		this.actions = actions;
	}
	
	public InternalAction[] getActions() {
		return actions;
	}
	
	@Override
	public String toString() {
		return Arrays.stream(actions).map(a -> a.toString()).collect(Collectors.joining(" (+) ", "(", ")"));
	}
}
