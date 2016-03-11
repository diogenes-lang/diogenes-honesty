package it.unica.co2.honesty.handlers;

import co2api.CO2ServerConnection;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;


class Participant_setConnection_Handler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("HANDLE -> PARTICIPANT SET CONNECTION");
		
		//object Participant
		ElementInfo participant = ti.getThisElementInfo().getModifiableInstance();
		
		ClassInfo connectionCI = ClassInfo.getInitializedClassInfo(CO2ServerConnection.class.getName(), ti);
		ElementInfo connectionEI = ti.getHeap().newObject(connectionCI, ti);
		
		participant.setReferenceField("connection", connectionEI.getObjectRef());
		
		Instruction nextInsn = new RETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

}
