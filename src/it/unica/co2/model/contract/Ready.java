package it.unica.co2.model.contract;

public class Ready extends Contract{

	private final ExternalAction action;

	public Ready(ExternalAction action) {
		this.action = action;
	}
	
	public Contract consumeAction() {
		return action.getNext();
	}
	
	@Override
	public String toString() {
		return "rdy ["+action.toString()+"]";
	}
}
