package it.unica.co2.model.contract;


public class Recursion extends Contract {

	private static final long serialVersionUID = 1L;
	
	private Contract contract;
	
	public Contract getContract() {
		return contract;
	}
	
	public Recursion setContract(Contract contract) {
		this.contract = contract;
		return this;
	}

	@Override
	public String toString() {
		return "<rec>";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contract == null) ? 0 : contract.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Recursion other = (Recursion) obj;
		if (contract == null) {
			if (other.contract != null)
				return false;
		} else if (!contract.equals(other.contract))
			return false;
		return true;
	}
	
}
