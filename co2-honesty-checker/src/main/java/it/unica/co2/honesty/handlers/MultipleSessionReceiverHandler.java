package it.unica.co2.honesty.handlers;

import java.util.Map;
import java.util.Map.Entry;

import co2api.ContractModel;
import co2api.SessionI;
import gov.nasa.jpf.jvm.JVMInstructionFactory;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.util.ObjectUtils;


class MultipleSessionReceiverHandler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("-- WAIT FOR RECEIVE (multi session) --");
		
		//parameters
		boolean timeout = listener.getArgumentInt(ti, 0)>0;
		log.info("timeout: "+timeout);
		
		ElementInfo mReceiver = ti.getThisElementInfo();
		
		// get the serialized map
		String serializedSessionActionsMap = mReceiver.getStringField("serializedSessionActionsMap");
		
		@SuppressWarnings("unchecked")
		Map<SessionI<? extends ContractModel>, Map<String, Integer>> sessionActionsMap = ObjectUtils.deserializeObjectFromStringQuietly(serializedSessionActionsMap, Map.class);
		
		// print some informations
		log.info("sessions: "+sessionActionsMap.size());
		

		for (Entry<SessionI<? extends ContractModel>, Map<String, Integer>> entry :  sessionActionsMap.entrySet()) {
			
			SessionI<? extends ContractModel> session = entry.getKey();
			Map<String, Integer> actions = entry.getValue();
			
			log.info("session: "+listener.getSessionID(session.getPublicContract().getUniqueID()));
			log.info("actions: "+actions);
		}
		
		ElementInfo consumersEI = mReceiver.getObjectField("consumersArray");
		int[] consumersRefs = consumersEI.asReferenceArray();

		log.info("number of consumers: "+consumersRefs.length);
		
		ElementInfo consumerEI = ti.getElementInfo(consumersRefs[0]);
		
		// create a message
//		ElementInfo message = getMessage(ti, "foo", "foo value");
		
		MethodInfo consumerMI = consumerEI.getClassInfo().getMethod("accept", "(Lco2api/Message;)V", false);
		
		log.info(consumerEI.toString());
		log.info(consumerMI.toString());
		
		// create a new frame with the message
		ti.getModifiableTopFrame().pushRef(consumerEI.getObjectRef());	// object ref
//		ti.getModifiableTopFrame().pushRef(message.getObjectRef());		// arguments
		
		
		Instruction nextInsn = JVMInstructionFactory.getFactory().invokespecial(consumerEI.getClassInfo().getName(), consumerMI.getName(), consumerMI.getSignature());
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

}
