package it.unica.co2.honesty.handlers;

import java.util.HashMap;
import java.util.Map;

public class HandlerFactory {

	private static Map<Class<? extends IHandler>, IHandler> handlers = new HashMap<>();
	
	private static <T extends IHandler> IHandler getHandler(Class<T> clazz) {
		
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
	
	public static IHandler tellHandler() {
		return getHandler(Participant_tell_Handler.class);
	}

	public static IHandler waitForSessionHandler() {
		return waitForSessionHandler(false);
	}
	
	public static IHandler waitForSessionHandler(boolean hasTimeout) {
		Public_waitForSession_Handler handler = (Public_waitForSession_Handler) getHandler(Public_waitForSession_Handler.class);
		handler.setTimeout(hasTimeout);
		return handler;
	}
	
	public static IHandler waitForReceiveHandler() {
		return getHandler(Session_waitForReceive_Handler.class);
	}
	
	public static IHandler ifThenElseHandler() {
		return getHandler(IfThenElseHandler.class);
	}
}
