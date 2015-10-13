package it.unica.co2.api.contract.utils;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.EmptyContract;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;

public class ContractFactory {
	
	public static EmptyContract empty() {
		return new EmptyContract();
	}
	
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
