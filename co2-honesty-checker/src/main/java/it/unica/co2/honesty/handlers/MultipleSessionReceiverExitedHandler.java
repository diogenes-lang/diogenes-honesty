package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;


public class MultipleSessionReceiverExitedHandler extends MethodHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi) {
		
		log.info("");
		log.info("-- WAIT FOR RECEIVE (method exited) -- ");
		
		log.info("getPC(): "+Co2Listener.insnToString(ti.getPC()));
		log.info("getNextPC(): "+Co2Listener.insnToString(ti.getNextPC()));
	}

}
