package it.unica.co2.model;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;


public class ContractFactory {
	
	public static Recursion recursion() {
		return new Recursion();
	}
	
	public static InternalSum internalSum() {
		return new InternalSum();
	}
	
	public static ExternalSum externalSum() {
		return new ExternalSum();
	}
	
	public static ContractWrapper wrapper() {
		return new ContractWrapper();
	}
	
	public static class ContractWrapper {

		private Contract contract;

		public Contract getContract() {
			return contract;
		}

		public void setContract(Contract contract) {
			this.contract = contract;
		}

	}
}
