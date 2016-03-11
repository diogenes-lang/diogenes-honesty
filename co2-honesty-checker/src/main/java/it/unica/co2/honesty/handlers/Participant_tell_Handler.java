package it.unica.co2.honesty.handlers;

import co2api.Public;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.NameProvider;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;
import it.unica.co2.util.ObjectUtils;


class Participant_tell_Handler extends InstructionHandlerA {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {
		log.info("");
		log.info("HANDLE -> TELL");

		//parameters
		String cserial = listener.getArgumentString(ti, 1);
		ElementInfo pvt = listener.getArgumentElementInfo(ti, 2);
		int delay = listener.getArgumentInteger(ti, 3);
		
		log.info("delay: "+delay);
		
		//get private fields (in order to build the public object)
		ElementInfo connection = ti.getElementInfo(pvt.getReferenceField("connection"));
		ElementInfo contract = ti.getElementInfo(pvt.getReferenceField("contract"));
		
		//get a unique ID for the contract (in order to handle delays appropriately when waitForSession is invoked)
		String contractID = NameProvider.getFreeName("c_");
		String sessionID = NameProvider.getFreeName("s_");
		
		log.info("binding contractID with sessionID: <"+contractID+","+sessionID+">");
		listener.associateContractToSession(contractID, sessionID);
		
		log.info("saving contract delay: <"+contractID+","+sessionID+">");
		listener.setContractDelay(contractID, delay>0);
		
		
		//build the return value
		ClassInfo pblCI = ClassInfo.getInitializedClassInfo(Public.class.getName(), ti);
		ElementInfo pblEI = ti.getHeap().newObject(pblCI, ti);
		
		pblEI.setReferenceField("connection", connection.getObjectRef());
		pblEI.setReferenceField("contract", contract.getObjectRef());
		pblEI.setReferenceField("uniqueID", ti.getHeap().newString(contractID, ti).getObjectRef());
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(pblEI.getObjectRef(), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
		
		
		
		
		
		TellDS tell = new TellDS();
		SumDS sum = new SumDS(tell);
		
		ContractDefinition cDef = ObjectUtils.deserializeObjectFromStringQuietly(cserial, ContractDefinition.class);
		listener.saveContract(contractID, cDef);
		
		tell.contractName = cDef.getName();
		tell.session = sessionID;
		
		tstate.setCurrentProcess(sum);		//set the current process
		tstate.setCurrentPrefix(tell);

		tstate.printInfo();

	}

}
