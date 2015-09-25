package it.unica.co2.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import it.unica.co2.api.contract.Action;

public class Utils {

	public static <T extends Action> Set<T> toSet(T[] actions) {
		
		return new HashSet<T>(Arrays.asList(actions));
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
