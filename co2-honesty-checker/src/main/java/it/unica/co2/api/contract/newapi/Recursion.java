package it.unica.co2.api.contract.newapi;


public class Recursion extends Contract {

	private static final long serialVersionUID = 1L;
	private final String name;
	private Contract contract;
	
	public Recursion(String name) {
		this.name=name;
	}
	
	public Recursion(Recursion rec) {
		this.name = rec.name;
		this.contract = rec.contract.deepCopy();
	}

	public String getName() {
		return name;
	}

	public Contract getContract() {
		return contract;
	}
	
	public Recursion setContract(Contract contract) {
		this.contract = contract;
		return this;
	}
	
	@Override
	public String toString() {
		return "REC ["+name+"-"+hashCode()+"] . {"+contract.toString()+"}";
	}
	
	@Override
	public Contract deepCopy() {
		return new Recursion(name).setContract(contract.deepCopy());
	}
}
