package it.unica.co2.api.contract;

import it.unica.co2.api.contract.utils.ContractExplorer;
import it.unica.co2.api.contract.utils.ContractFactory;

public class Recursion extends SessionType {

	private static final long serialVersionUID = 1L;
	private final String name;
	private SessionType contract;
	
	public Recursion(String name) {
		this.name=name;
	}
	
	public String getName() {
		return name;
	}

	public SessionType getContract() {
		return contract;
	}
	
	public Recursion setContract(SessionType contract) {
		
		this.contract = contract;
		
		ContractExplorer.findAll(
				contract, 
				SessionType.class,
				(x)->{
					return x==this;
				},
				(x)->{
					throw new IllegalArgumentException("the given contract contains a Recursion referring to this object (infinite loop)");
				});
		
		return this;
	}
	
	@Override
	public String toString() {
		return "REC ["+name+"-"+hashCode()+"] . {"+contract.toString()+"}";
	}
	
	@Override
	public SessionType deepCopy() {
		
		SessionType cCopy = contract.deepCopy();
		Recursion copy = new Recursion(name).setContract(cCopy);
		
		ContractExplorer.findAll(	// fix recursion references
				cCopy, 
				RecursionReference.class,
				(x)->(x.getReference()==this),
				(x)->{x.getPreceeding().next(ContractFactory.recRef(copy));});
		
		return copy;
	}
}
