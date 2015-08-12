package it.unica.co2.model.contract;

public class ContractWrapper extends Contract {
	
	private static final long serialVersionUID = 1L;
	private Contract contract;

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}
	
	@Override
	public String toString() {
		return contract.toString();
	}
}
