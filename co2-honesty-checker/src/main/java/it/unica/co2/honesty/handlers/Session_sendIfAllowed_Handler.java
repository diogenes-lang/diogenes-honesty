package it.unica.co2.honesty.handlers;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.DoSendDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;


class Session_sendIfAllowed_Handler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {
		log.info("");
		log.info("-- SEND --");
		
		/*
		 * collect the co2 process
		 */
		ElementInfo session2 = ti.getThisElementInfo();
		
		String sessionName = listener.getSessionIDBySession(ti, session2);
		String action = listener.getArgumentString(ti, 0);
		
		DoSendDS send = new DoSendDS();
		send.session = sessionName;
		send.action = action;
		send.sort = Sort.unit();
		
//		if (insn.getMethodInfo()==Session_sendIfAllowed) send.sort = Sort.unit();
//		if (insn.getMethodInfo()==Session_sendIfAllowedInt) send.sort = Sort.integer();
//		if (insn.getMethodInfo()==Session_sendIfAllowedString) send.sort = Sort.string();
		
		SumDS sum = new SumDS();
		sum.prefixes.add(send);
		
		log.info("setting current process: "+sum);
		
		tstate.setCurrentProcess(sum);		//set the current process
		tstate.setCurrentPrefix(send);		//set the current prefix
		tstate.printInfo();
		
		/*
		 * handle return value
		 */
		ClassInfo booleanCI = ClassInfo.getInitializedClassInfo(Boolean.class.getName(), ti);
		ElementInfo booleanEI = ti.getHeap().newObject(booleanCI, ti);
		
		booleanEI.setBooleanField("value", true);
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(booleanEI.getObjectRef(), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

}
