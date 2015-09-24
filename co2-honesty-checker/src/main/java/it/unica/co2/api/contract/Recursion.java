package it.unica.co2.api.contract;


public class Recursion extends Contract {

	private static final long serialVersionUID = 1L;
	
	private Contract contract;
	
	public Contract getContract() {
		return contract;
	}
	
	public Recursion setContract(Contract contract) {
		this.contract = contract;
		return this;
	}

	@Override
	public String toString() {
		return "<rec> <...>";
	}
}
