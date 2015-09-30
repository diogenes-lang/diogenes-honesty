package it.unica.co2.api.contract.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;


public class ContractExplorer {

	public static <T extends Contract> List<T> findall(Contract contract, Class<T> clazz) {
		return findall(contract, clazz, (x)->{ return true;}, (x)->{});
	}
	
	public static <T extends Contract> List<T> findall(Contract contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		List<T> contracts = new ArrayList<>();
		findall(new HashSet<>(), contract, clazz, predicate, contracts, consumer);
		return contracts;
	}
	
	private static <T extends Contract> void findall(Set<Contract> visited, Contract contract, Class<T> clazz, Predicate<T> predicate, List<T> contracts, Consumer<T> consumer) {
		
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
				findall(visited, a.getNext(), clazz, predicate, contracts, consumer);
			}
		
		else if(contract instanceof ExternalSum)
			for (ExternalAction a1 : ((ExternalSum) contract).getActions()) {
				findall(visited, a1.getNext(), clazz, predicate, contracts, consumer);
			}
		
		else if(contract instanceof Recursion)
			findall(visited, ((Recursion) contract).getContract(), clazz, predicate, contracts, consumer);
		
	}

	
	public static Set<ContractDefinition> getAllReferences(ContractDefinition c) {
		
		Set<ContractDefinition> accumulator = new HashSet<>();
		getReferences(c, accumulator);
		
		return accumulator;
	}
	
	private static void getReferences(ContractDefinition c, Set<ContractDefinition> acc) {
		
		List<ContractReference> refs = ContractExplorer.findall(
				c.getContract(), 
				ContractReference.class
			);
		
		for (ContractReference cRef : refs) {
			
			if (acc.contains(cRef.getReference())) {
				// acc contains cRef.getReference() and all its references
			}
			else {
				acc.add(cRef.getReference());
				getReferences(cRef.getReference(), acc);
			}
		}
		
	}
}
