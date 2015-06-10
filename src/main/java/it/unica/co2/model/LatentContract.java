package it.unica.co2.model;

import it.unica.co2.model.contract.Contract;

public class LatentContract extends CO2System {

	private Contract contract;
	private final Partecipant partecipant;

	public LatentContract(Contract contract, Partecipant partecipant) {
		this.contract = contract;
		this.partecipant = partecipant;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public Partecipant getPartecipant() {
		return partecipant;
	}
	
}
