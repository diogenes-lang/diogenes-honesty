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
	protected ChoiceGenerator<ThreadInfo> getRunnableCG(String id, ThreadInfo tiCurrent) {
		ApplicationContext appCtx = tiCurrent.getApplicationContext();
		ThreadInfo[] choices = getTimeoutRunnables(appCtx);

		if (choices.length == 0) {
			return null;
		}

		if ((choices.length == 1) && (choices[0] == tiCurrent) && !tiCurrent.isTimeoutWaiting()) { // no
																									// context
																									// switch
			if (!breakSingleChoice) {
				return null;
			}
		}

		ChoiceGenerator<ThreadInfo> cg = new ThreadChoiceFromSet(id, new ThreadInfo[] { choices[0] }, true);

		if (!vm.getThreadList().hasProcessTimeoutRunnables(appCtx)) {
			GlobalSchedulingPoint.setGlobal(cg);
		}

		return cg;
	}
	
	
	@Override
	public boolean setsBlockedThreadCG(ThreadInfo ti, ElementInfo ei) {
		return false;
	}

	@Override
	public boolean setsLockAcquisitionCG(ThreadInfo ti, ElementInfo ei) {
		return false;
	}

	@Override
	public boolean setsLockReleaseCG(ThreadInfo ti, ElementInfo ei, boolean didUnblock) {
		return false;
	}

	// --- thread termination
	@Override
	public boolean setsTerminationCG(ThreadInfo ti) {
		return setBlockingCG(TERMINATE, ti);
	}

	// --- java.lang.Object APIs
	@Override
	public boolean setsWaitCG(ThreadInfo ti, long timeout) {
		return false;
	}

	@Override
	public boolean setsNotifyCG(ThreadInfo ti, boolean didNotify) {
		return false;
	}

	@Override
	public boolean setsNotifyAllCG(ThreadInfo ti, boolean didNotify) {
		return false;
	}

	// --- the java.lang.Thread APIs
	@Override
	public boolean setsStartCG(ThreadInfo tiCurrent, ThreadInfo tiStarted) {
		return false;
	}

	@Override
	public boolean setsYieldCG(ThreadInfo ti) {
		return false;
	}

	@Override
	public boolean setsPriorityCG(ThreadInfo ti) {
		return false;
	}

	@Override
	public boolean setsSleepCG(ThreadInfo ti, long millis, int nanos) {
		return false;
	}

	@Override
	public boolean setsSuspendCG(ThreadInfo tiCurrent, ThreadInfo tiSuspended) {
		return false;
	}

	@Override
	public boolean setsResumeCG(ThreadInfo tiCurrent, ThreadInfo tiResumed) {
		return false;
	}

	@Override
	public boolean setsJoinCG(ThreadInfo tiCurrent, ThreadInfo tiJoin, long timeout) {
		return false;
	}

	@Override
	public boolean setsStopCG(ThreadInfo tiCurrent, ThreadInfo tiStopped) {
		return false;
	}

	@Override
	public boolean setsInterruptCG(ThreadInfo tiCurrent, ThreadInfo tiInterrupted) {
		return false;
	}

	// --- sun.misc.Unsafe
	@Override
	public boolean setsParkCG(ThreadInfo ti, boolean isAbsTime, long timeout) {
		return false;
	}

	@Override
	public boolean setsUnparkCG(ThreadInfo tiCurrent, ThreadInfo tiUnparked) {
		return false;
	}

	  
	// --- system scheduling events

	/**
	 * this one has to be guaranteed to set a CG
	 */
	@Override
	public void setRootCG() {
		ThreadInfo[] runnables = vm.getThreadList().getTimeoutRunnables();
		ChoiceGenerator<ThreadInfo> cg = new ThreadChoiceFromSet(ROOT, runnables, true);
		vm.getSystemState().setMandatoryNextChoiceGenerator(cg, "no ROOT choice generator");
	}

	// --- gov.nasa.jpf.vm.Verify
	@Override
	public boolean setsBeginAtomicCG(ThreadInfo ti) {
		return false;
	}

	@Override
	public boolean setsEndAtomicCG(ThreadInfo ti) {
		return false;
	}

	// --- ThreadInfo reschedule request
	@Override
	public boolean setsRescheduleCG(ThreadInfo ti, String reason) {
		return false;
	}

	// --- FinalizerThread
	@Override
	public boolean setsPostFinalizeCG(ThreadInfo tiFinalizer) {
		return false;
	}
}
