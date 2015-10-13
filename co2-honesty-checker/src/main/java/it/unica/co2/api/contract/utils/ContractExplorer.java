package it.unica.co2.api.contract.utils;

import java.util.HashSet;
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

	public static <T extends Contract> void findAll(Contract contract, Class<T> clazz) {
		findAll(contract, clazz, (x)->{});
	}
	
	public static <T extends Contract> void findAll(Contract contract, Class<T> clazz, Consumer<T> consumer) {
		findAll(contract, clazz, (x)->(true), consumer);
	}
	
	public static <T extends Contract> void findAll(Contract contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		Set<T> acc = new HashSet<>();
		findAll(new HashSet<>(), contract, clazz, predicate, acc);
		
		for (T c : acc) {
			consumer.accept(c);
		}
	}
	
	private static <T extends Contract> void findAll(Set<Contract> visited, Contract contract, Class<T> clazz, Predicate<T> predicate, Set<T> acc) {
		
		if (contract==null)
			return;
		
		if (visited.contains(contract))
			return;
		
		visited.add(contract);	//add to visited

		if (clazz.isInstance(contract) && predicate.test(clazz.cast(contract))) {
			acc.add(clazz.cast(contract));
		}
		
		//continue searching
		if (contract instanceof InternalSum)
			for (InternalAction a : ((InternalSum) contract).getActions()) {
				findAll(visited, a.getNext(), clazz, predicate, acc);
			}
		
		else if(contract instanceof ExternalSum)
			for (ExternalAction a1 : ((ExternalSum) contract).getActions()) {
				findAll(visited, a1.getNext(), clazz, predicate, acc);
			}
		
		else if(contract instanceof Recursion)
			findAll(visited, ((Recursion) contract).getContract(), clazz, predicate, acc);
		
	}

	
	public static Set<ContractDefinition> getAllReferences(ContractDefinition c) {
		
		Set<ContractDefinition> accumulator = new HashSet<>();
		getReferences(c, accumulator);
		
		return accumulator;
	}
	
	private static void getReferences(ContractDefinition c, Set<ContractDefinition> acc) {
		
		ContractExplorer.findAll(
				c.getContract(), 
				ContractReference.class,
				(x)->{
					if (acc.contains(x.getReference())) {
						// acc contains cRef.getReference() and all its references
					}
					else {
						acc.add(x.getReference());
						getReferences(x.getReference(), acc);
					}
				}
			);
	}
}
