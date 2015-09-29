package it.unica.co2.api.contract.newapi;


public class RecursionReference extends Contract {

	private static final long serialVersionUID = 1L;
	
	private final Recursion reference;
	
	public RecursionReference(RecursionReference ref) {
		this.reference = new Recursion(ref.getReference());
	}

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
		return new RecursionReference(this);
	}
}
