package it.unica.co2.semantics;

import it.unica.co2.model.contract.Action;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.CtxAction;
import it.unica.co2.model.contract.Ready;
import it.unica.co2.model.contract.Recursion;

public class ContextAbstractContractConfiguration {
	
	private final Contract contract;
	
	private ContextAbstractContractConfiguration(Contract contract) {
		if (contract instanceof Recursion) {
			contract = ContractSemantics.expandRecursion((Recursion)contract);
			assert !(contract instanceof Recursion);
		}
		
		this.contract = contract;
	}

	public Contract getContract() {
		return contract;
	}
	
	public static ContextAbstractContractConfiguration convert(Participant p, ContractConfiguration cc) {
		
		Contract c = p==Participant.A? cc.getA(): cc.getB();
		
		if (c instanceof Ready) {
			Ready rdy = (Ready) c;
			return new ContextAbstractContractConfiguration(new CtxAction(Action.toInternal(rdy.getAction())));
		}
		else {
			return new ContextAbstractContractConfiguration(c);
		}
		
	}
}
