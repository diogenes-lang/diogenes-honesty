package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;


public class RunEnvProcessExitedHandler extends MethodHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi) {
		
		ClassInfo ci = ti.getExecutingClassInfo();

		/*
		 * the process is finished
		 */
		log.info("");
		tstate.printInfo();
		log.info("--RUN ENV PROCESS-- (method exited) -> "+ci.getSimpleName());
		
		tstate.tryToPopFrame();
		
		//next flag prevent from re-build the process at each invocation
		listener.getEnvProcess(ci.getSimpleName()).alreadyBuilt = true;
		
		tstate.printInfo();
	}

}
