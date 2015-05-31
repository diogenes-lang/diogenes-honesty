package it.unica.co2.model.contract;

public class Recursion extends Contract {

	private Contract contract;
	
	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	@Override
	public String toString() {
		return "<rec>";
	}
}
