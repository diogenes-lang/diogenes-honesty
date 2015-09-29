package it.unica.co2.api.contract.newapi;


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
		return reference.getName();
	}

	@Override
	public Contract deepCopy() {
		return new ContractReference(reference);
	}
}
