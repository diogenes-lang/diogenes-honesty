package it.unica.co2.api.contract;

public class ContractDefinition {
	
	private final String name;
	private Contract contract;
	
	public ContractDefinition(String name) {
		this.name = name;
	}

//	public ContractDefinition(ContractDefinition def) {
//		this.name = def.name;
//		this.contract = def.contract.deepCopy();
//		
//		new ContractExplorer().findall(
//				contract, 
//				ContractReference.class, 
//				(x)->(x.getReference()==def), 
//				(x)->{x.getPreceeding().next(new ContractReference(this));}
//				);
//	}

	public String getName() {
		return name;
	}
	
	public Contract getContract() {
		return contract;
	}

	public ContractDefinition setContract(Contract contract) {
		this.contract = contract;
		return this;
	}
	
	@Override
	public String toString() {
		return "DEF ["+name +"-"+hashCode()+"] = "+ contract.toString();
	}

}
