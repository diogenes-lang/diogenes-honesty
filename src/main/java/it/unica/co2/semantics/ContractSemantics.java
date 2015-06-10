package it.unica.co2.semantics;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Ready;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractTransition.Partecipant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContractSemantics {


	public static Contract expandRecursion(Recursion recursion) {
		return recursion.getContract();
	}
	
	public static ContractTransition intExt(Partecipant p, String actionName, InternalSum intSum, ExternalSum extSum) {
		
//		InternalAction[] intActions = Arrays.stream(intSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(InternalAction[]::new);
//		ExternalAction[] extActions = Arrays.stream(extSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(ExternalAction[]::new);
		
		InternalAction[] intActions = Utils.filter(intSum.getActions(), actionName);
		ExternalAction[] extActions = Utils.filter(extSum.getActions(), actionName);
		
		assert intActions.length==1;
		assert extActions.length==1;
		
		InternalAction a = intActions[0];
		ExternalAction b = extActions[0];
		
		ContractConfiguration cc = null;
		
		if (p==Partecipant.A) {
			cc = new ContractConfiguration(a.getNext(), new Ready(b));
		}
		else if (p==Partecipant.B) {
			cc = new ContractConfiguration(new Ready(b), a.getNext());
		}
		else {
			throw new AssertionError("unexpected branch");
		}
		
		ContractTransition t = new ContractTransition(p, actionName, cc);
		cc.setPrecedingTransition(t);
		
		return t;
	}
	
	public static ContractTransition rdy(Partecipant p, Ready ready, Contract c) {
		
		ContractConfiguration cc = null;
		
		if (p==Partecipant.A) {
			cc = new ContractConfiguration(ready.consumeAction(), c);
		}
		else if (p==Partecipant.B) {
			cc = new ContractConfiguration(c, ready.consumeAction());
		}
		else {
			throw new AssertionError("unexpected branch");
		}
		
		ContractTransition t = new ContractTransition(p, ready.getActionName(), cc);
		cc.setPrecedingTransition(t);
		
		return t;
	}

	public static ContractTransition[] getNextTransitions(ContractConfiguration contractConfiguration) {

		List<ContractTransition> result = new ArrayList<>();
		
		Contract a = contractConfiguration.getA();
		Contract b = contractConfiguration.getB();
		
		if (a instanceof Recursion) {
			a = expandRecursion((Recursion)a);
			assert !(a instanceof Recursion);
		}
		
		if (b instanceof Recursion) {
			b = expandRecursion((Recursion)b);
			assert !(b instanceof Recursion);
		}
		
		if (a instanceof Ready) {
			result.add( rdy( Partecipant.A, (Ready)a, b ) );		// apply rdy
		}
		
		if (b instanceof Ready) {
			result.add( rdy( Partecipant.B, (Ready)b, a ) );		// apply rdy
		}
		
		
		
		if (
				(a instanceof InternalSum && b instanceof ExternalSum) ||
				(a instanceof ExternalSum && b instanceof InternalSum)
				) {
			
			Partecipant p = a instanceof InternalSum? Partecipant.A : Partecipant.B;
			
			InternalSum intSum = ((InternalSum) (a instanceof InternalSum? a:b));
			ExternalSum extSum = ((ExternalSum) (a instanceof ExternalSum? a:b));
			
			InternalAction[] intActions = intSum.getActions();
			ExternalAction[] extActions = extSum.getActions();
			
			if (intActions.length!=0) {
				
			
//				Set<String> intActionsSet = Arrays.stream(intActions).map( action -> action.getName() ).collect(Collectors.toSet());
//				Set<String> extActionsSet = Arrays.stream(extActions).map( action -> action.getName() ).collect(Collectors.toSet());

				Set<String> intActionsSet = Utils.toSet(intActions);
				Set<String> extActionsSet = Utils.toSet(extActions);
	
				for (String intAction : intActionsSet) {
					
					if (extActionsSet.contains(intAction)) {
						
							result.add( intExt(p, intAction, intSum, extSum) );	// apply int-ext
					}
				}
			
			}
		}
		
		return result.toArray(new ContractTransition[]{});
	}
	
}
