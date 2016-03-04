package it.unica.co2.honesty;

import java.util.HashMap;
import java.util.Map;

public class NameProvider {

	private static Map<String, Integer> names = new HashMap<>();
	
	
	public static String getFreeName() {
		return getFreeName("");
	}
	
	public static String getFreeName(Object salt) {
		return getFreeName("", salt);
	}
	
	public static String getFreeName(String prefix) {
		return getFreeName(prefix, null);
	}
	
	public static String getFreeName(String prefix, Object salt) {
		
		String key = prefix + (salt!=null? salt.hashCode(): "");
		
		names.merge(key, 0, (x, y) -> (x+1));
		
		String name = key + names.get(key);
		
		return name;
	}
	
	
}
