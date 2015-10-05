package it.unica.co2.api.contract;


public class RecursionReference extends Contract {

	private static final long serialVersionUID = 1L;
	
	private final Recursion reference;
	
	public RecursionReference(Recursion reference) {
		this.reference=reference;
	}

	public Recursion getReference() {
		return reference;
	}

	@Override
	public String toString() {
		return "ref["+reference.getName()+"-"+reference.hashCode()+"]";
	}
	
	@Override
	public Contract deepCopy() {
		return new RecursionReference(reference);
	}
}