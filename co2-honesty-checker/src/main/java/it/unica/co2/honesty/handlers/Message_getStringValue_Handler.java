package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;


class Message_getStringValue_Handler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		//bypass the type check of the message
		
		ElementInfo message = ti.getThisElementInfo();
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(message.getReferenceField("stringVal"), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

}
