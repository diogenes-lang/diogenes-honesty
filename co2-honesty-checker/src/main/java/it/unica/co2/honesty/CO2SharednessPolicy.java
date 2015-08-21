package it.unica.co2.honesty;

import gov.nasa.jpf.vm.ApplicationContext;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.SharednessPolicy;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;


public class CO2SharednessPolicy implements SharednessPolicy {

	@Override
	public void initializeSharednessPolicy(VM vm, ApplicationContext appCtx) {

	}

	@Override
	public void initializeObjectSharedness(ThreadInfo allocThread, DynamicElementInfo ei) {

	}

	@Override
	public void initializeClassSharedness(ThreadInfo allocThread, StaticElementInfo ei) {

	}

	@Override
	public boolean canHaveSharedObjectCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi) {
		return false;
	}

	@Override
	public ElementInfo updateObjectSharedness(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
		return ei;
	}

	@Override
	public boolean setsSharedObjectCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi) {
		return false;
	}

	@Override
	public boolean canHaveSharedClassCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi) {
		return false;
	}

	@Override
	public ElementInfo updateClassSharedness(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
		return ei;
	}

	@Override
	public boolean setsSharedClassCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi) {
		return false;
	}

	@Override
	public boolean canHaveSharedArrayCG(ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx) {
		return false;
	}

	@Override
	public ElementInfo updateArraySharedness(ThreadInfo ti, ElementInfo eiArray, int idx) {
		return eiArray;
	}

	@Override
	public boolean setsSharedArrayCG(ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx) {
		return false;
	}

	@Override
	public boolean setsSharedObjectExposureCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed) {
		return false;
	}

	@Override
	public boolean setsSharedClassExposureCG(ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, ElementInfo eiExposed) {
		return false;
	}

	@Override
	public void cleanupThreadTermination(ThreadInfo ti) {

	}

}
