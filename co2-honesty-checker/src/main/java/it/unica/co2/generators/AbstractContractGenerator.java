package it.unica.co2.generators;

import java.util.Collection;

import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.contract.newapi.ContractReference;
import it.unica.co2.api.contract.newapi.ExternalAction;
import it.unica.co2.api.contract.newapi.ExternalSum;
import it.unica.co2.api.contract.newapi.InternalAction;
import it.unica.co2.api.contract.newapi.InternalSum;
import it.unica.co2.api.contract.newapi.Recursion;
import it.unica.co2.api.contract.newapi.RecursionReference;

public abstract class AbstractContractGenerator {

	protected Contract contract;

	public AbstractContractGenerator() {
		super();
	}

	public AbstractContractGenerator(Contract c) {
		this.contract=c;
	}

	public String generate() {
		return convert(contract);
	}
	
	protected String convert(Contract contract) {
		
		if (contract instanceof InternalSum)
			return convert((InternalSum) contract);
		
		else if(contract instanceof ExternalSum)
			return convert((ExternalSum) contract);
		
		else if(contract instanceof Recursion)
			return convert((Recursion) contract);
		
		else if(contract instanceof RecursionReference)
			return convert((RecursionReference) contract);
		
		else if(contract instanceof ContractReference)
			return convert((ContractReference) contract);
		
		throw new AssertionError("Unexpected behaviour");
	}
	
	protected abstract String convert(InternalSum contract);
	
	protected abstract String convert(ExternalSum contract);
	
	protected abstract String convert(InternalAction action);
	
	protected abstract String convert(ExternalAction action);
	
	protected abstract String convert(Recursion recursion);
	
	protected String convert(RecursionReference ref) {
		return ref.getReference().getName();
	}
	
	protected String convert(ContractReference ref) {
		return ref.getReference().getName();
	}
	
	public Collection<String> getRecursionNames() {
		return null;
	}
	
}