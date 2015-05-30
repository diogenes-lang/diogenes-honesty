package it.unica.co2.semantics;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Ready;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ContractSemantics {

	
	public static ContractConfiguration intExt(String actionName, InternalSum intSum, ExternalSum extSum) {
		
		InternalAction[] intActions = Arrays.stream(intSum.getActions()).filter( a -> a.getName().equals(actionName)).toArray(InternalAction[]::new);
		ExternalAction[] extActions = Arrays.stream(extSum.getActions()).filter( a -> a.getName().equals(actionName)).toArray(ExternalAction[]::new);
		
		assert intActions.length==1;
		assert extActions.length==1;
		
		InternalAction a = intActions[0];
		ExternalAction b = extActions[0];
		
		return new ContractConfiguration(a.getNext(), new Ready(b));
	}
	
	public static ContractConfiguration rdy(Ready ready, Contract c) {
		
		return new ContractConfiguration(ready.consumeAction(), c);
	}


	public static ContractConfiguration[] getNextConfiguration(ContractConfiguration contractConfiguration) {
		
		List<ContractConfiguration> result = new ArrayList<>();
		
		Contract a = contractConfiguration.getA();
		Contract b = contractConfiguration.getB();
		
		if (a instanceof Ready) {
			result.add( rdy( (Ready)a, b ) );		// apply rdy
		}
		
		if (b instanceof Ready) {
			result.add( rdy( (Ready)b, a ) );		// apply rdy
		}
		
		if (
				(a instanceof InternalSum && b instanceof ExternalSum) ||
				(b instanceof InternalSum && a instanceof ExternalSum)
				) {
			
			InternalSum intSum = ((InternalSum) (a instanceof InternalSum? a:b));
			ExternalSum extSum = ((ExternalSum) (a instanceof ExternalSum? a:b));
			
			InternalAction[] intActions = intSum.getActions();
			ExternalAction[] extActions = extSum.getActions();
			
			if (intActions.length!=0) {
				
			
				Set<String> intActionsSet = Arrays.stream(intActions).map( action -> action.getName() ).collect(Collectors.toSet());
				Set<String> extActionsSet = Arrays.stream(extActions).map( action -> action.getName() ).collect(Collectors.toSet());
	
				for (String intAction : intActionsSet) {
					
					if (extActionsSet.contains(intAction)) {
						
						result.add( intExt(intAction, intSum, extSum) );	// apply int-ext
					}
				}
			
			}
		}
		
		return result.toArray(new ContractConfiguration[]{});
	}
	
}
