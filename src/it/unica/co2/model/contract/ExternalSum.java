package it.unica.co2.model.contract;

import java.util.Arrays;
import java.util.stream.Collectors;


public class ExternalSum extends Contract{

	private final ExternalAction[] actions;

	public ExternalSum(ExternalAction... actions) {
		this.actions = actions;
	}
	
	public ExternalAction[] getActions() {
		return actions;
	}
	
	@Override
	public String toString() {
		return Arrays.stream(actions).map(a -> a.toString()).collect(Collectors.joining(" + ", "[", "]"));
	}
}
