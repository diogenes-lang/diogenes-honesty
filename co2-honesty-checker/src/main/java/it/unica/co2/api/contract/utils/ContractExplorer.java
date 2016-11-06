package it.unica.co2.api.contract.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;


/**
 * Utility class to navigate within a {@code SessionType}.
 * 
 * @author Nicola Atzei
 */
public class ContractExplorer {

	/**
	 * Starting from {@code contract}, recursively find all the occurrences of type {@code clazz}.
	 * Then, apply the given {@code consumer} to each element.
	 * 
	 * @param contract The search's starting point.
	 * @param clazz The class each element have to instance.
	 * @param consumer The consumer to apply to each element.
	 */
	public static <T extends SessionType> void findAll(SessionType contract, Class<T> clazz, Consumer<T> consumer) {
		findAll(contract, clazz, (x)->(true), consumer);
	}
	
	/**
	 * Starting from {@code contract}, recursively find all the occurrences of type {@code clazz} that satisfy {@code predicate}.
	 * Then, apply the given {@code consumer} to each element.
	 * 
	 * @param contract The search's starting point.
	 * @param clazz The class each element have to instance.
	 * @param predicate The predicate each element have to satisfy.
	 * @param consumer The consumer to apply to each element.
	 */
	public static <T extends SessionType> void findAll(SessionType contract, Class<T> clazz, Predicate<T> predicate, Consumer<T> consumer) {
		Set<T> acc = new HashSet<>();
		findAll(new HashSet<>(), contract, clazz, predicate, acc);
		
		for (T c : acc) {
			consumer.accept(c);
		}
	}
	
	/*
	 * Internal implementation of findAll():
	 * - it keeps track of the visited nodes, accumulating the result into a set.
	 * - the search stops if come across ContractReference or RecursionReference (to avoid loops)
	 */
	private static <T extends SessionType> void findAll(Set<SessionType> visited, SessionType contract, Class<T> clazz, Predicate<T> predicate, Set<T> acc) {
		
		if (contract==null)
			return;
		
		if (visited.contains(contract))
			return;
		
		visited.add(contract);	//add to visited

		if (clazz.isInstance(contract) && predicate.test(clazz.cast(contract))) {
			acc.add(clazz.cast(contract));
		}
		
		//continue searching
		if (contract instanceof InternalSum) {
			for (InternalAction a : ((InternalSum) contract).getActions()) {
				findAll(visited, a.getNext(), clazz, predicate, acc);
			}
		}
		else if(contract instanceof ExternalSum) {
			for (ExternalAction a1 : ((ExternalSum) contract).getActions()) {
				findAll(visited, a1.getNext(), clazz, predicate, acc);
			}
		}
		else if(contract instanceof Recursion) {
			findAll(visited, ((Recursion) contract).getContract(), clazz, predicate, acc);
		}
	}

	/**
	 * Starting from {@code contract}, find all the {@code ContractDefinition} occurrences (including the given one).
	 * 
	 * @param contract The search's starting point.
	 * @return All the occurrences of {@code ContractDefinition}.
	 */
	public static Set<ContractDefinition> getAllReferences(ContractDefinition contract) {
		
		Set<ContractDefinition> accumulator = new HashSet<>();
		getReferences(contract, accumulator);
		
		return accumulator;
	}
	
	/*
	 * Internal implementation:
	 * - first search for all ContractReferences
	 * - recursively continue searching
	 */
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
