package it.unica.co2.honesty.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.handlers.MethodHandler.MethodWrapper;

public abstract class MethodHandler implements HandlerI<MethodWrapper>{

	protected final Logger log;
	
	protected MethodHandler() {
		log = JPF.getLogger(this.getClass().getName());
		log.setLevel(Level.ALL);
	}
	
	public static class MethodWrapper {
		public Co2Listener listener;
		public ThreadInfo ti;
		public MethodInfo mi;
	}
	
	@Override
	public void handle(MethodWrapper obj) {
		handle(obj.listener, obj.listener.getThreadState(obj.ti), obj.ti, obj.mi);
	}
	
	abstract public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi);
}
