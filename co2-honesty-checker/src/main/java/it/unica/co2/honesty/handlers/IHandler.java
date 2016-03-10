package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;

public interface IHandler<T> {

	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn);
}
