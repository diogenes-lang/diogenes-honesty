package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.ParallelProcessesDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;


public class ParallelMethodEnteredHandler extends MethodHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi) {
		log.info("");
		log.info("--PARALLEL-- (method entered) -> ID:"+tstate.getId());
		
		ParallelProcessesDS parallel = new ParallelProcessesDS();
		
		SumDS sumA = new SumDS();
		PrefixPlaceholderDS placeholderA = new PrefixPlaceholderDS();
		sumA.prefixes.add(placeholderA);
		
		SumDS sumB = new SumDS();
		PrefixPlaceholderDS placeholderB = new PrefixPlaceholderDS();
		sumB.prefixes.add(placeholderB);
		
		parallel.processA = sumA;	
		parallel.processB = sumB;	
		
		tstate.setCurrentProcess(parallel);
		tstate.setCurrentPrefix(placeholderB);			// the caller continues to append here
		
		// set the entry points of the next thread
		listener.setCurrentThreadProcess(sumA);			
		listener.setCurrentThreadPrefix(placeholderA);	// the called continues to append here
	}

}
