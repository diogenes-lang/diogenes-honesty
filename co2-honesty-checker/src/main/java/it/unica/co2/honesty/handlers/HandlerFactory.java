package it.unica.co2.honesty.handlers;

import java.util.HashMap;
import java.util.Map;

public class HandlerFactory {

	private static Map<Class<? extends HandlerI<?>>, HandlerI<?>> handlers = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	private static <T extends HandlerI<?>> T getHandler(Class<T> clazz) {
		
		if (!handlers.containsKey(clazz)) {
			
			try {
				handlers.put(clazz, clazz.newInstance());
				return clazz.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
		HandlerI<?> handler = handlers.get(clazz);
		
		if (clazz.isInstance(handler))	
			return (T) handlers.get(clazz);
		else
			throw new IllegalStateException("seems that you store a type '"+handler.getClass()+"' instead of '"+clazz.getName()+"'");
	}
	
	public static InstructionHandler tellHandler() {
		return getHandler(Participant_tell_Handler.class);
	}

	public static InstructionHandler waitForSessionHandler() {
		return waitForSessionHandler(false);
	}
	
	public static InstructionHandler waitForSessionHandler(boolean hasTimeout) {
		Public_waitForSession_Handler handler = (Public_waitForSession_Handler) getHandler(Public_waitForSession_Handler.class);
		handler.setTimeout(hasTimeout);
		return handler;
	}
	
	public static InstructionHandler waitForReceiveHandler() {
		return getHandler(Session_waitForReceive_Handler.class);
	}
	
	public static InstructionHandler messageHandler() {
		return getHandler(Message_getStringValue_Handler.class);
	}
	
	public static InstructionHandler loggerFactoryHandler() {
		return getHandler(LoggerFactory_getLogger_Handler.class);
	}
	
	public static InstructionHandler ifThenElseHandler() {
		return getHandler(IfThenElseHandler.class);
	}

	public static InstructionHandler sendIfAllowedHandler() {
		return getHandler(Session_sendIfAllowed_Handler.class);
	}
	
	public static InstructionHandler setConnectionHandler() {
		return getHandler(Participant_setConnection_Handler.class);
	}
	
	public static InstructionHandler multipleSessionReceiverHandler() {
		return getHandler(MultipleSessionReceiverHandler.class);
	}
	
	public static InstructionHandler skipMethodHandler() {
		return getHandler(SkipMethodHandler.class);
	}
	
	public static MethodHandler parallelEnteredHandler() {
		return getHandler(ParallelMethodEnteredHandler.class);
	}
}
