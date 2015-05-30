package it.unica.co2.model.contract;

public class Recursion extends Contract {

	private final String recursionName;
	private final Contract contract;

	public Recursion(String recursionName, Contract contract) {
		this.recursionName = recursionName;
		this.contract = contract;
	}

	public String getRecursionName() {
		return recursionName;
	}

	public Contract getContract() {
		return contract;
	}

}
