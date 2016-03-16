package it.unica.co2.honesty.handlers;

import java.util.ArrayList;
import java.util.List;

import co2api.ContractExpiredException;
import co2api.Session;
import co2api.TimeExpiredException;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.IntChoiceFromList;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.AskDS;
import it.unica.co2.honesty.dto.CO2DataStructures.RetractDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;


class Public_waitForSession_Handler extends InstructionHandler {

	private boolean hasTimeout = false;
	
	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("-- WAIT FOR SESSION --");
		
		//object Public
		ElementInfo pbl = ti.getThisElementInfo();
		
		
		String contractID = pbl.getStringField("uniqueID");
		String sessionID = listener.getSessionID(contractID);
		
		log.info("contractID: "+contractID);
		log.info("sessionID: "+sessionID);
		
		boolean hasDelay = listener.contractHasDelay(contractID);
		
		List<Integer> choiceSet = new ArrayList<>();
		choiceSet.add(0);
		
		if (hasDelay) choiceSet.add(1);
		if (hasTimeout) choiceSet.add(2);
		
		if (choiceSet.size()>1) {
			log.info("considering multiple choices");
			
			if (!ti.isFirstStepInsn()) {
				
				log.info("TOP-HALF");
				
				SumDS sum = new SumDS();
				
				tstate.setCurrentProcess(sum);		//set the current process
				tstate.printInfo();
				
				/*
				 * the co2CurrentPrefix is set in methodExited
				 */
				log.info("pushing the sum onto the stack");
				
				if (hasTimeout && hasDelay) {
					log.info("timeout and delay");
					tstate.pushSum(sum, "ask", "t", "retract");
				}
				else if (hasTimeout) {
					log.info("timeout");
					tstate.pushSum(sum, "ask", "t");
				}
				else if (hasDelay) {
					log.info("delay");
					tstate.pushSum(sum, "ask", "retract");
				}
				else {
					throw new IllegalStateException();
				}
				
				
				
				IntChoiceFromList cg = new IntChoiceFromList(tstate.getWaitForSessionChoiceGeneratorName(), choiceSet.stream().mapToInt(i -> i).toArray());
				cg.setAttr(sum);
				
				ti.getVM().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				return;
			}
			else {
				log.info("BOTTOM-HALF");
				
				// get the choice generator
				IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(tstate.getWaitForSessionChoiceGeneratorName(), IntChoiceFromList.class);

				if (!cg.hasMoreChoices()) {
					// this is the last choice, we can pop the sum pushed on top-half
				}
				
				
				SumDS sum = (SumDS) cg.getAttr();
				
				
				
				// take a choice
				switch (cg.getNextChoice()) {
				case 0:
					
					log.info("returning a new Session");
					
					AskDS ask = new AskDS(sessionID);
					sum.prefixes.add(ask);
					
					log.info("setting current prefix: "+ask);
					tstate.setCurrentPrefix(ask);
					tstate.printInfo();
					tstate.popSum(ask);

					//build the return value
					ClassInfo sessionCI = ClassInfo.getInitializedClassInfo(Session.class.getName(), ti);
					ElementInfo sessionEI = ti.getHeap().newObject(sessionCI, ti);
					
					sessionEI.setReferenceField("connection", pbl.getReferenceField("connection"));
					sessionEI.setReferenceField("contract", pbl.getObjectRef());
					sessionEI.setReferenceField("sessionID", ti.getHeap().newString(sessionID, ti).getObjectRef());
					
					//set the return value
					StackFrame frame = ti.getTopFrame();
					frame.setReferenceResult(sessionEI.getObjectRef(), null);
					
					Instruction nextInsn = new ARETURN();
					nextInsn.setMethodInfo(insn.getMethodInfo());
					
					ti.skipInstruction(nextInsn);
					
					return;

				case 1:
					log.info("delay expired");
					
					RetractDS retract = new RetractDS();
					retract.session = sessionID;
					sum.prefixes.add(retract);
					
					log.info("setting current prefix: "+retract);
					tstate.setCurrentPrefix(retract);
					tstate.printInfo();
					tstate.popSum(retract);
					
					log.info("delay expired, throwing a ContractExpiredException");
					
					// get the exception ClassInfo
					ClassInfo ci = ClassInfo.getInitializedClassInfo(ContractExpiredException.class.getName(), ti);
					
					// create the new exception and push on the top stack
					StackFrame sf = ti.getModifiableTopFrame(); 
					sf.push(ti.getHeap().newObject(ci, ti).getObjectRef());
					ATHROW athrow = new ATHROW();
					
					//schedule the next instruction
					ti.skipInstruction(athrow);
					return;

				case 2:
					log.info("timeout expired");
					
					TauDS tau = new TauDS(); 
					sum.prefixes.add(tau);
					
					log.info("setting current prefix: "+tau);
					tstate.setCurrentPrefix(tau);
					tstate.printInfo();
					tstate.popSum(tau);
					
					log.info("timeout expired, throwing a TimeExpiredException");
					
					// get the exception ClassInfo
					ClassInfo ci2 = ClassInfo.getInitializedClassInfo(TimeExpiredException.class.getName(), ti);
					
					// create the new exception and push on the top stack
					ti.getModifiableTopFrame().push(ti.getHeap().newObject(ci2, ti).getObjectRef());
					ATHROW athrow2 = new ATHROW();
					
					//schedule the next instruction
					ti.skipInstruction(athrow2);
					return;
				}
			}
		}
		else {
			log.info("single choice");

			AskDS ask = new AskDS(sessionID);
			
			SumDS sum = new SumDS();
			sum.prefixes.add(ask);
			
			log.info("setting current process: "+sum);
			log.info("setting current prefix: "+ask);
			tstate.setCurrentProcess(sum);		//set the current process
			tstate.setCurrentPrefix(ask);
			tstate.printInfo();
			
			log.info("returning a new Session");
			
			//build the return value
			ClassInfo sessionCI = ClassInfo.getInitializedClassInfo(Session.class.getName(), ti);
			ElementInfo sessionEI = ti.getHeap().newObject(sessionCI, ti);
			
			sessionEI.setReferenceField("connection", pbl.getReferenceField("connection"));
			sessionEI.setReferenceField("contract", pbl.getObjectRef());
			sessionEI.setReferenceField("sessionID", ti.getHeap().newString(sessionID, ti).getObjectRef());
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(sessionEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
	}

	public boolean hasTimeout() {
		return hasTimeout;
	}

	public void setTimeout(boolean hasTimeout) {
		this.hasTimeout = hasTimeout;
	}

}
