package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.ThreadState;

public interface IHandler {

	public void handle(ThreadState tstate, ThreadInfo ti, Instruction insn);
}
