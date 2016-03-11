package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;


class LoggerFactory_getLogger_Handler extends InstructionHandlerA {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {
		log.info("");
		log.info("HANDLE -> LOGGERFACTORY GET LOGGER");
		
		ClassInfo loggerCI = ClassInfo.getInitializedClassInfo(org.slf4j.helpers.NOPLogger.class.getName(), ti);
		ElementInfo loggerEI = ti.getHeap().newObject(loggerCI, ti);
		
		//set the return value
		log.info("loggerEI: "+loggerEI);
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(loggerEI.getObjectRef(), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

}
