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

	public static <T extends Contract> void findall(Contract contract, Class<T> clazz) {
		findAll(contract, clazz, (x)->{});
	}
	
	public static <T extends Contract> void findAll(Contract contract, Class<T> clazz, Consumer<T> consumer) {
		findAll(contract, clazz, (x)->(true), consumer);
	}
	
	public static <T extends Contract> void findAll(Contract contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		findall(new HashSet<>(), contract, clazz, predicate, consumer);
	}
	
	private static <T extends Contract> void findall(Set<Contract> visited, Contract contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		
		if (contract==null)
			return;
		
		if (visited.contains(contract))
			return;
		
		visited.add(contract);	//add to visited

		if (clazz.isInstance(contract) && predicate.test(clazz.cast(contract))) {
			consumer.accept(clazz.cast(contract));
		}
		
		//continue searching
		if (contract instanceof InternalSum)
			for (InternalAction a : ((InternalSum) contract).getActions()) {
				findall(visited, a.getNext(), clazz, predicate, consumer);
			}
		
		else if(contract instanceof ExternalSum)
			for (ExternalAction a1 : ((ExternalSum) contract).getActions()) {
				findall(visited, a1.getNext(), clazz, predicate, consumer);
			}
		
		else if(contract instanceof Recursion)
			findall(visited, ((Recursion) contract).getContract(), clazz, predicate, consumer);
		
		else if(contract instanceof ContractReference)
			findall(visited, ((ContractReference) contract).getReference().getContract(), clazz, predicate, consumer);
		
	}

	
	public static Set<ContractDefinition> getAllReferences(ContractDefinition c) {
		
		Set<ContractDefinition> accumulator = new HashSet<>();
		getReferences(c, accumulator);
		
		return accumulator;
	}
	
	private static void getReferences(ContractDefinition c, Set<ContractDefinition> acc) {
		
		List<ContractReference> refs = new ArrayList<>();
		
		ContractExplorer.findAll(
				c.getContract(), 
				ContractReference.class,
				(x)->{
					refs.add(x);
				}
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
