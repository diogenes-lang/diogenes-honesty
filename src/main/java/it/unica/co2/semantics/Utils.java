package it.unica.co2.semantics;

import it.unica.co2.model.contract.Action;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

public class Utils {

	public static Set<String> toSet(Action[] actions) {
		
		Set<String> result = new HashSet<>();
		
		for (Action a : actions) {
			result.add(a.getName());
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Action> T[] filter(T[] actions, String actionName) {
		
		Set<T> result = new HashSet<>();
		
		for (T a : actions) {
			if (actionName.equals(a.getName()))
				result.add(a);
		}
		
		Class<?> clazz = actions.getClass().getComponentType();
		int length = result.size();
		
		return result.toArray( (T[]) Array.newInstance(clazz, length));
	}
}
