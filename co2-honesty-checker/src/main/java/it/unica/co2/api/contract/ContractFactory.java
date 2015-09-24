package it.unica.co2.api.contract;

public class ContractFactory {
	
	public static Recursion recursion() {
		return new Recursion();
	}
	
	public static InternalSum internalSum() {
		return new InternalSum();
	}
	
	public static ExternalSum externalSum() {
		return new ExternalSum();
	}
	
	public static ContractWrapper wrapper() {
		return new ContractWrapper();
	}
	
}
