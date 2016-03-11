package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.handlers.InstructionHandlerI.InstructionWrapper;

public interface InstructionHandlerI extends HandlerI<InstructionWrapper>{

	public static class InstructionWrapper {
		public Instruction insn;
		public ThreadInfo ti;
		public Co2Listener listener;
	}
	
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn);
}
