package it.unica.co2.semantics;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Ready;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.lts.LTSState;
import it.unica.co2.semantics.lts.LTSTransition;

import java.util.Set;

public class ContractConfiguration implements LTSState {

	private static final long serialVersionUID = 1L;
	
	private final Contract a;
	private final Contract b;
	private LTSTransition transition;
	
	public ContractConfiguration(Contract a, Contract b) {
		
		if (a instanceof Recursion) {
			a = ContractSemantics.expandRecursion((Recursion)a);
			assert !(a instanceof Recursion);
		}
		
		if (b instanceof Recursion) {
			b = ContractSemantics.expandRecursion((Recursion)b);
			assert !(b instanceof Recursion);
		}
		
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
			
//			Set<String> intActionsSet = Arrays.stream(intActions).map( action -> action.getName() ).collect(Collectors.toSet());
//			Set<String> extActionsSet = Arrays.stream(extActions).map( action -> action.getName() ).collect(Collectors.toSet());

			Set<String> intActionsSet = Utils.toSet(intActions);
			Set<String> extActionsSet = Utils.toSet(extActions);
			
			// intActionsSet ⊆ extActionsSet
			if (extActionsSet.containsAll(intActionsSet))
				return true;
			else
				return false;
		}
		
		return false;
	}

	
	
	/*
	 * The transition between one contract-configuration to another is defined by the abstract-semantics of contracts
	 */
	private ContractTransition[] nextStates = null;
	
	@Override
	public LTSTransition[] getAvailableTransitions() {
		return nextStates;
	}

	@Override
	public LTSTransition getPrecededTransition() {
		return transition;
	}
	
	@Override
	public void setPrecedingTransition(LTSTransition transition) {
		this.transition = transition;
	}
	
	@Override
	public boolean hasNext() {
		
		if (nextStates==null) {
			nextStates = ContractSemantics.getNextTransitions(this);
		}

		assert nextStates!=null;
		
		return nextStates.length!=0;
	}
	
	@Override
	public boolean check() {
		return isSafe();
	}
	
	@Override
	public String toString() {
		return "A: ["+a+"]   |   B: ["+b+"]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
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
		ContractConfiguration other = (ContractConfiguration) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

}
