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

public class ContractSemantics {

	
	public static ContractConfiguration intExt(String actionName, InternalSum intSum, ExternalSum extSum) {
		
//		InternalAction[] intActions = Arrays.stream(intSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(InternalAction[]::new);
//		ExternalAction[] extActions = Arrays.stream(extSum.getActions()).filter( x -> x.getName().equals(actionName)).toArray(ExternalAction[]::new);
		
		InternalAction[] intActions = Utils.filter(intSum.getActions(), actionName);
		ExternalAction[] extActions = Utils.filter(extSum.getActions(), actionName);
		
		assert intActions.length==1;
		assert extActions.length==1;
		
		InternalAction a = intActions[0];
		ExternalAction b = extActions[0];
		
		return new ContractConfiguration(a.getNext(), new Ready(b));
	}
	
	
	
	public static ContractConfiguration intExt(String actionName, ExternalSum extSum, InternalSum intSum) {

		ContractConfiguration tmp = intExt(actionName, intSum, extSum);
		return new ContractConfiguration(tmp.getB(), tmp.getA());
	}
	
	
	
	public static ContractConfiguration rdy(Ready ready, Contract c) {
		return new ContractConfiguration(ready.consumeAction(), c);
	}

	public static ContractConfiguration rdy(Contract c, Ready ready) {
		return new ContractConfiguration(c, ready.consumeAction());
	}
	
	
	
	public static Contract expandRecursion(Recursion recursion) {
		return recursion.getContract();
	}
	
	
	
	public static ContractConfiguration[] getNextConfiguration(ContractConfiguration contractConfiguration) {
		
		List<ContractConfiguration> result = new ArrayList<>();
		
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
			result.add( rdy( (Ready)a, b ) );		// apply rdy
		}
		
		if (b instanceof Ready) {
			result.add( rdy( a, (Ready)b ) );		// apply rdy
		}
		
		
		
		if (
				(a instanceof InternalSum && b instanceof ExternalSum) ||
				(a instanceof ExternalSum && b instanceof InternalSum)
				) {
			
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
						
						if (a instanceof InternalSum)							//to maintain the order in the configuration
							result.add( intExt(intAction, intSum, extSum) );	// apply int-ext
						else
							result.add( intExt(intAction, extSum, intSum) );	// apply int-ext
					}
				}
			
			}
		}
		
		return result.toArray(new ContractConfiguration[]{});
	}
	
}
