package it.unica.co2.compliance;

import it.unica.co2.lts.LTSState;
import it.unica.co2.lts.LTSTransition;

public class ContractTransition implements LTSTransition {

	private static final long serialVersionUID = 1L;

	private final Participant partecipant;
	private final String actionName;
	private final ContractConfiguration contractConfiguration;
	
	public ContractTransition(Participant partecipant, String actionName, ContractConfiguration contractConfiguration) {
		this.partecipant = partecipant;
		this.actionName = actionName;
		this.contractConfiguration = contractConfiguration;
	}

	public Participant getPartecipant() {
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
