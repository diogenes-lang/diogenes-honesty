package it.unica.co2.api.contract.defeq;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.contract.newapi.ExternalAction;
import it.unica.co2.api.contract.newapi.ExternalSum;
import it.unica.co2.api.contract.newapi.InternalAction;
import it.unica.co2.api.contract.newapi.InternalSum;
import it.unica.co2.api.contract.newapi.Recursion;


public class ContractExplorer {

	private Set<Contract> visited = new HashSet<>();
	
	
	public <T extends Contract> List<T> findall(Contract contract, Class<T> clazz) {
		return findall(contract, clazz, (x)->{ return true;}, (x)->{});
	}
	
	public <T extends Contract> List<T> findall(Contract contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		List<T> contracts = new ArrayList<>();
		findall(contract, clazz, predicate, contracts, consumer);
		return contracts;
	}
	
	private <T extends Contract> void findall(Contract contract, Class<T> clazz, Predicate<T> predicate, List<T> contracts, Consumer<T> consumer) {
		
		if (contract==null)
			return;
		
		if (visited.contains(contract))
			return;
		
		visited.add(contract);	//add to visited

		if (clazz.isInstance(contract) && predicate.test(clazz.cast(contract))) {
			contracts.add(clazz.cast(contract));	//add to collection
			consumer.accept(clazz.cast(contract));
		}
		
		//continue searching
		if (contract instanceof InternalSum)
			for (InternalAction a : ((InternalSum) contract).getActions()) {
				findall(a.getNext(), clazz, predicate, contracts, consumer);
			}
		
		else if(contract instanceof ExternalSum)
			for (ExternalAction a1 : ((ExternalSum) contract).getActions()) {
				findall(a1.getNext(), clazz, predicate, contracts, consumer);
			}
		
		else if(contract instanceof Recursion)
			findall(((Recursion) contract).getContract(), clazz, predicate, contracts, consumer);
		
//		else if(contract instanceof ContractReference)
//			findall(((ContractReference) contract).getReference().getContract(), clazz, predicate, contracts, consumer);
		
	}

}
