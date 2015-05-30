package it.unica.co2.semantics;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Ready;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ContractConfiguration implements LTSState<ContractConfiguration>{

	private final Contract a;
	private final Contract b;
	
	public ContractConfiguration(Contract a, Contract b) {
		this.a = a;
		this.b = b;
	}

	public Contract getA() {
		return a;
	}

	public Contract getB() {
		return b;
	}
	
	boolean isSafe() {
		
		// A : 0  |  B : 0
		if (a==null && b==null) {
			return true;
		}
		
		// A : rdy a?v.c  |  B : d 
		if (
				a instanceof Ready || 
				b instanceof Ready
				) {
			return true;
		}
		
		// A : intsum { a_i ! T_i . c_i }  |  B : extsum { b_j ? T_j . c_j }  with  ∅ ≠ {a_i}_i ⊆ {a_j}_j
		if (
				(a instanceof InternalSum && b instanceof ExternalSum) ||
				(b instanceof InternalSum && a instanceof ExternalSum)
				) {
			
			InternalAction[] intActions = ((InternalSum) (a instanceof InternalSum? a:b)).getActions();
			ExternalAction[] extActions = ((ExternalSum) (a instanceof ExternalSum? a:b)).getActions();
			
			if (intActions.length==0)
				return false;
			
			Set<String> intActionsSet = Arrays.stream(intActions).map( action -> action.getName() ).collect(Collectors.toSet());
			Set<String> extActionsSet = Arrays.stream(extActions).map( action -> action.getName() ).collect(Collectors.toSet());

			for (String intAction : intActionsSet) {
				
				if (!extActionsSet.contains(intAction)) {
					return false;
				}
			}
			
			// intActionsSet ⊆ extActionsSet
			return true;
		}
		
		return false;
	}

	
	
	/*
	 * The transition between one contract-configuration to another is defined by the semantics of contracts
	 */
	private ContractConfiguration[] nextStates = null;
	
	@Override
	public boolean hasNext() {
		
		if (nextStates==null) {
			nextStates = ContractSemantics.getNextConfiguration(this);
		}

		assert nextStates!=null;
		
		return nextStates.length!=0;
	}

	@Override
	public LTSState<ContractConfiguration>[] nextStates() {
		return nextStates;
	}
	
	
	@Override
	public String toString() {
		return "A: ["+a+"]   |   B: ["+b+"]";
	}
	
}
