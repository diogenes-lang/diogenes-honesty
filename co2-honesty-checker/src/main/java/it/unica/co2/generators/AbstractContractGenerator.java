package it.unica.co2.generators;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ContractWrapper;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContractGenerator {

	protected Contract contract;
	private char count = 'x';
	protected Map<Recursion,String> recursions = new HashMap<Recursion,String>();

	public AbstractContractGenerator() {
		super();
	}

	public AbstractContractGenerator(Contract c) {
		this.contract=c;
	}

	protected String getRecursionName() {
		return "x"+count++;
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
		else if(contract instanceof ContractWrapper)
			return convert((ContractWrapper) contract);
		
		throw new AssertionError("Unexpected behaviour");
	}
	
	protected abstract String convert(InternalSum contract);
	
	protected abstract String convert(ExternalSum contract);
	
	protected abstract String convert(InternalAction action);
	
	protected abstract String convert(ExternalAction action);
	
	protected abstract String convert(Recursion recursion);
	
	protected String convert(ContractWrapper wrapper) {
		return this.convert(wrapper.getContract());
	}
	
	public Collection<String> getRecursionNames() {
		return recursions.values();
	}
	
}