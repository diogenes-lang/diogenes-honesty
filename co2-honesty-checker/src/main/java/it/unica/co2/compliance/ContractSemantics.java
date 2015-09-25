package it.unica.co2.compliance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Ready;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.util.Utils;

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
	public static ContractTransition intExt(Participant p, InternalAction intAction, ExternalAction extAction) {
		
		ContractConfiguration cc = null;
		
		if (p==Participant.A) {
			cc = new ContractConfiguration(intAction.getNext(), new Ready(extAction));
		}
		else if (p==Participant.B) {
			cc = new ContractConfiguration(new Ready(extAction), intAction.getNext());
		}
		else {
			throw new AssertionError("unexpected branch");
		}
		
		ContractTransition t = new ContractTransition(p, intAction.getName(), cc);
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
			
			
			
			Set<InternalAction> intActionsSet = Utils.toSet(intActions);
			Set<ExternalAction> extActionsSet = Utils.toSet(extActions);

			for (InternalAction intAction : intActionsSet) {
				
				for (ExternalAction extAction : extActionsSet) {
					
					if (
							intAction.getName().equals(extAction.getName()) &&		//same name
							intAction.getSort() == extAction.getSort()				//same sort
							)
						result.add( intExt(p, intAction, extAction) );	// apply int-ext
				}
			}
		}
		
		return result.toArray(new ContractTransition[]{});
	}
	
}
