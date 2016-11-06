package it.unica.co2.api.contract.serializer;

import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.EmptyContract;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.SessionType;

/**
 * Abstract class for serializing a {@code SessionType}.
 * 
 * @author Nicola Atzei
 */
public abstract class AbstractContractSerializer {

	/**
	 * Serialize the given contract.
	 * 
	 * @param contract The contract to serialize.
	 * @return The serialized contract.
	 */
	public String convert(SessionType contract) {

		// dispatching
		
		if (contract instanceof InternalSum) {
			return convert((InternalSum) contract);
		}
		else if(contract instanceof ExternalSum) {
			return convert((ExternalSum) contract);
		}
		else if(contract instanceof Recursion) {
			return convert((Recursion) contract);
		}
		else if(contract instanceof RecursionReference) {
			return convert((RecursionReference) contract);
		}
		else if(contract instanceof ContractReference) {
			return convert((ContractReference) contract);
		}
		else if(contract instanceof EmptyContract) {
			return convert((EmptyContract) contract);
		}
		
		throw new IllegalStateException("Unexpected contract "+contract.getClass());
	}
	
	protected abstract String convert(InternalSum contract);
	
	protected abstract String convert(ExternalSum contract);
	
	protected abstract String convert(InternalAction action);
	
	protected abstract String convert(ExternalAction action);
	
	protected abstract String convert(Recursion recursion);
	
	protected abstract String convert(EmptyContract recursion);
	
	protected abstract String convert(RecursionReference ref);
	
	protected abstract String convert(ContractReference ref);
	
}