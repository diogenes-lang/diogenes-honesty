package it.unica.co2.api.contract.generators;

import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.EmptyContract;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.SessionType;

public abstract class AbstractContractGenerator {

	protected SessionType contract;

	public AbstractContractGenerator() {
		super();
	}

	public AbstractContractGenerator(SessionType c) {
		this.contract=c;
	}

	public String generate() {
		return convert(contract);
	}
	
	protected String convert(SessionType contract) {
		
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
		
		else if(contract instanceof EmptyContract)
			return convert((EmptyContract) contract);
		
		throw new IllegalStateException("Unexpected contract "+contract.getClass());
	}
	
	protected abstract String convert(InternalSum contract);
	
	protected abstract String convert(ExternalSum contract);
	
	protected abstract String convert(InternalAction action);
	
	protected abstract String convert(ExternalAction action);
	
	protected abstract String convert(Recursion recursion);
	
	protected abstract String convert(EmptyContract recursion);
	
	protected String convert(RecursionReference ref) {
		return ref.getReference().getName();
	}
	
	protected String convert(ContractReference ref) {
		return ref.getReference().getName();
	}
	
}