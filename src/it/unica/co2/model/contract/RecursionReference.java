package it.unica.co2.model.contract;

public class RecursionReference extends Contract {

	private final String referredRecursionName;

	public RecursionReference(String referredRecursionName) {
		this.referredRecursionName = referredRecursionName;
	}

	public String getReferredRecursionName() {
		return referredRecursionName;
	}
	
}
