package it.unica.co2.api.contract.newapi;

public class ContractFactory {
	
	public static InternalSum internalSum() {
		return new InternalSum();
	}
	
	public static ExternalSum externalSum() {
		return new ExternalSum();
	}
	
	public static ContractDefinition def(String name) {
		return new ContractDefinition(name);
	}

	public static Recursion recursion(String name) {
		return new Recursion(name);
	}
	
	public static ContractReference ref(ContractDefinition c) {
		return new ContractReference(c);
	}
	
	public static RecursionReference recRef(Recursion rec) {
		return new RecursionReference(rec);
	}
}
