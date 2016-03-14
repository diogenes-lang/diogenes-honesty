package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;


public class RunEnvProcessEnteredHandler extends MethodHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi) {
		
		ClassInfo ci = ti.getExecutingClassInfo();
		
		/*
		 * the process is finished
		 */
		log.info("");
		tstate.printInfo();
		log.info("--RUN ENV PROCESS-- (method entered) -> "+ci.getSimpleName());
		
		/*
		 * Check for recursive behavior
		 */
		ProcessDefinitionDS proc = listener.getEnvProcess(ci.getSimpleName());

		boolean recursiveCall = tstate.checkForRecursion(proc);
		
		if (recursiveCall || proc.alreadyBuilt) {
			
			if (recursiveCall) {
				// the call is recursive: stop search
				log.info("recursive call detected, terminating");
			}
			
			if (proc.alreadyBuilt) {
				// the process was already called
				// the flag is set when called process returns (exit of the 'run' method)
				log.info("process already built: "+proc.toString());
			}
			
			log.info("[SKIP] [T-ID "+tstate.getId()+"] adding method "+mi.getFullName());
			listener.addMethodToSkip(mi);
		}
		else {
			log.info("NOT recursive call AND NOT already built");
		}
		
		log.info("adding a new process onto the stack: "+proc.toString());
		tstate.pushNewFrame(proc);
		tstate.printInfo();
	}

}
