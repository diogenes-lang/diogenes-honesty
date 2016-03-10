package it.unica.co2.honesty.handlers;

import java.util.HashMap;
import java.util.Map;

public class HandlerFactory {

	private static Map<Class<? extends IHandler>, IHandler> handlers = new HashMap<>();
	
	public static <T extends IHandler> IHandler getHandler(Class<T> clazz) {
		
		if (!handlers.containsKey(clazz)) {
			
			try {
				handlers.put(clazz, clazz.newInstance());
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
		return handlers.get(clazz);
	}
	
}
