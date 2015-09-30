package it.unica.co2.api.contract;


public class ContractReference extends Contract {

	private static final long serialVersionUID = 1L;
	
	private final ContractDefinition reference;

	public ContractReference(ContractDefinition reference) {
		this.reference=reference;
	}

	public ContractDefinition getReference() {
		return reference;
	}

	@Override
	public String toString() {
		return "cref["+reference.getName() +"-"+reference.hashCode()+"]";
	}

	@Override
	public Contract deepCopy() {
		return new ContractReference(reference);
	}
}
