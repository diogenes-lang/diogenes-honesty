package it.unica.co2.model;

import it.unica.co2.semantics.ContractConfiguration;


public class Session extends CO2System {

	private final ContractConfiguration contractConfiguration;

	public Session(ContractConfiguration contractConfiguration) {
		this.contractConfiguration = contractConfiguration;
	}

	public ContractConfiguration getContractConfiguration() {
		return contractConfiguration;
	}
	
}
