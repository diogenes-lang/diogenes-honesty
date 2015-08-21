package it.unica.co2.honesty;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.AllRunnablesSyncPolicy;
import gov.nasa.jpf.vm.ApplicationContext;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.GlobalSchedulingPoint;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

public class CO2SyncPolicy extends AllRunnablesSyncPolicy {

	public CO2SyncPolicy(Config config) {
		super(config);
	}

	@Override
	public boolean setsStartCG(ThreadInfo tiCurrent, ThreadInfo tiStarted) {
		return false;
	}

	@Override
	public boolean setsTerminationCG(ThreadInfo tiCurrent) {
		ChoiceGenerator<ThreadInfo> cg;

		ApplicationContext appCtx = tiCurrent.getApplicationContext();
		ThreadInfo[] choices = getTimeoutRunnables(appCtx);

		if (choices.length == 0) {
			cg = null;
		}

		if ((choices.length == 1) && (choices[0] == tiCurrent) && !tiCurrent.isTimeoutWaiting()) { // no context switch
			if (!breakSingleChoice) {
				cg = null;
			}
		}

		cg = new ThreadChoiceFromSet("TERMINATE", new ThreadInfo[] { choices[0] }, true);

		if (!vm.getThreadList().hasProcessTimeoutRunnables(appCtx)) {
			GlobalSchedulingPoint.setGlobal(cg);
		}

		return setNextChoiceGenerator(cg);
	}
	
	@Override
	public boolean setsLockAcquisitionCG(ThreadInfo ti, ElementInfo ei) {
		return false;
	}
}
