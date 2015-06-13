package it.unica.co2.semantics;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Ready;
import it.unica.co2.model.contract.Recursion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Semantics of value-abstract contracts.
 */
public class ContractSemantics {

	/**
	 * Expand the <code>Recursion</code> contract.
	 * @param recursion
	 * @return the body (contract) of the recursion.
	 * @see Recursion
	 */
	public static Contract expandRecursion(Recursion recursion) {
		return recursion.getContract();
	}
	
	/**
	 * <p>Perform the <code>actionName</code> action. <code>InternalSum</code> and <code>ExternalSum</code> must contain
	 * an action with this name (multiple actions with the same name is not allowed by the CO2-syntax). The participant is
	 * useful only to preserve contracts order in the <code>ContractConfiguration</code> (returned by 
	 * {@link ContractTransition#apply()}).</p>
	 * <p>The resulting <code>ContractConfiguration</code> is composed using the involved actions. The internal action was consumed
	 * and we get the following contract. The external action is used to create the <code>Ready</code> contract.</p>
	 * 
	 * @param p the participant that own the internal sum
	 * @param actionName the name of the action
	 * @param intSum the internal sum (owned by p)
	 * @param extSum the external sum
	 * @return the <code>ContractTransition</code> that fires the action p:actionName
	 * 
	 * @see ContractTransition
	 * @see ContractConfiguration
	 */
	public static ContractTransition intExt(Participant p, String actionName, InternalSum intSum, ExternalSum extSum) {
		
//		InternalAction[] intActions = Arrays.stream(intSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(InternalAction[]::new);
//		ExternalAction[] extActions = Arrays.stream(extSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(ExternalAction[]::new);
		
		InternalAction[] intActions = Utils.filter(intSum.getActions(), actionName);
		ExternalAction[] extActions = Utils.filter(extSum.getActions(), actionName);
		
		assert intActions.length==1;
		assert extActions.length==1;
		
		InternalAction a = intActions[0];
		ExternalAction b = extActions[0];
		
		ContractConfiguration cc = null;
		
		if (p==Participant.A) {
			cc = new ContractConfiguration(a.getNext(), new Ready(b));
		}
		else if (p==Participant.B) {
			cc = new ContractConfiguration(new Ready(b), a.getNext());
		}
		else {
			throw new AssertionError("unexpected branch");
		}
		
		ContractTransition t = new ContractTransition(p, actionName, cc);
		cc.setPrecedingTransition(t);
		
		return t;
	}
	
	/**
	 * Consume the action of the <code>Ready</code> contract. The participant is
	 * useful only to preserve contracts order in the <code>ContractConfiguration</code> (returned by 
	 * {@link ContractTransition#apply()}).
	 * 
	 * @param p the participant that own the ready contract
	 * @param ready the ready contract
	 * @param c the other contract
	 * @return the <code>ContractTransition</code> that fires the action p:{@link Ready#getActionName()}
	 * 
	 * @see ContractTransition
	 * @see ContractConfiguration
	 */
	public static ContractTransition rdy(Participant p, Ready ready, Contract c) {
		
		ContractConfiguration cc = null;
		
		if (p==Participant.A) {
			cc = new ContractConfiguration(ready.consumeAction(), c);
		}
		else if (p==Participant.B) {
			cc = new ContractConfiguration(c, ready.consumeAction());
		}
		else {
			throw new AssertionError("unexpected partecipant");
		}
		
		ContractTransition t = new ContractTransition(p, ready.getActionName(), cc);
		cc.setPrecedingTransition(t);
		
		return t;
	}

	/**
	 * Get all <code>ContractTransition</code> allowed by the passed <code>ContractConfiguration</code>.
	 * 
	 * @param contractConfiguration
	 * @return the array of all possible transitions
	 * 
	 * @see ContractTransition
	 * @see ContractConfiguration
	 */
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
			result.add( rdy( Participant.A, (Ready)a, b ) );		// apply rdy
		}
		
		if (b instanceof Ready) {
			result.add( rdy( Participant.B, (Ready)b, a ) );		// apply rdy
		}
		
		
		
		if (
				(a instanceof InternalSum && b instanceof ExternalSum) ||
				(a instanceof ExternalSum && b instanceof InternalSum)
				) {
			
			Participant p = a instanceof InternalSum? Participant.A : Participant.B;
			
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
