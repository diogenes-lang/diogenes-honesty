package it.unica.co2.semantics;

public class ContractTransition implements LTSTransition {

	private static final long serialVersionUID = 1L;

	public static enum Partecipant {A,B}
	
	private final Partecipant partecipant;
	private final String actionName;
	private final ContractConfiguration contractConfiguration;
	
	public ContractTransition(Partecipant partecipant, String actionName, ContractConfiguration contractConfiguration) {
		this.partecipant = partecipant;
		this.actionName = actionName;
		this.contractConfiguration = contractConfiguration;
	}

	public Partecipant getPartecipant() {
		return partecipant;
	}
	
	public String getActionName() {
		return actionName;
	}

	@Override
	public LTSState apply() {
		return contractConfiguration;
	}
	
	@Override
	public String toString() {
		return partecipant+": "+actionName;
	}
}
