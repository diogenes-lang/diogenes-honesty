package it.unica.co2.api.contract;

import java.io.Serializable;

public class ContractDefinition implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	private SessionType contract;
	
	public ContractDefinition(String name) {
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public SessionType getContract() {
		return contract;
	}

	public ContractDefinition setContract(SessionType contract) {
		this.contract = contract;
		return this;
	}
	
	@Override
	public String toString() {
		return "DEF ["+name +"-"+hashCode()+"] = "+ contract.toString();
	}
	
	public String getId() {
		return "DEF ["+name +"-"+hashCode()+"]";
	}
}
