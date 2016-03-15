package it.unica.co2.honesty.handlers;

import java.util.ArrayList;
import java.util.List;

import co2api.TimeExpiredException;
import gov.nasa.jpf.jvm.JVMStackFrame;
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
import it.unica.co2.honesty.dto.CO2DataStructures.DoReceiveDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;


class Session_waitForReceive_Handler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("-- WAIT FOR RECEIVE --");
		
		ElementInfo sessionEI = ti.getThisElementInfo();
		
		String sessionID = listener.getSessionIDBySession(ti, sessionEI);
		String contractID = listener.getContractIDBySession(ti, sessionEI);
		
		//parameters
		boolean timeout = listener.getArgumentInteger(ti, 0)>0;
		List<String> actions = listener.getArgumentStringArray(ti, 1);
		
		assert actions.size()>=1 : "you must pass at least one action";
		
		log.info("timeout: "+timeout);
		log.info("actions: "+actions);
		
		if (timeout || actions.size()>1) {
			log.info("considering multiple choices");
			
			
			if (!ti.isFirstStepInsn()) {
				
				log.info("TOP-HALF");
				
				SumDS sum = new SumDS();
				
				if (timeout) {
					String[] arr = new String[actions.size()+1];
					arr[0] = "t";
					actions.stream().forEach((x)->{arr[actions.indexOf(x)+1]=x;});
					tstate.pushSum(sum, arr);		//set the current process
				}
				else
					tstate.pushSum(sum, actions.toArray(new String[]{}));
					
				tstate.setCurrentProcess(sum);
				tstate.printInfo();
				
				List<Integer> choiceSet = new ArrayList<>();
				
				actions.stream().forEach((x)-> {choiceSet.add(actions.indexOf(x));});
				if (timeout) choiceSet.add(actions.size());
				
				assert choiceSet.size()>1;
				assert choiceSet.size()==actions.size() || choiceSet.size()-1==actions.size();

				IntChoiceFromList cg = new IntChoiceFromList(tstate.getWaitForReceiveChoiceGeneratorName(), choiceSet.stream().mapToInt(i -> i).toArray());
				ti.getVM().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				return;
			}
			else {
				
				log.info("BOTTOM-HALF");
				
				SumDS sum = tstate.getSum();
				
				// get the choice generator
				IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(tstate.getWaitForReceiveChoiceGeneratorName(), IntChoiceFromList.class);
				
				assert cg!=null : "choice generator not found: "+tstate.getWaitForReceiveChoiceGeneratorName();
				
				// take a choice
				int choice = cg.getNextChoice();
				
				if (choice==actions.size()) {
					
					log.info("timeout expired");
					
					TauDS tau = new TauDS();
					sum.prefixes.add(tau);
					
					log.info("setting current prefix: "+tau);
					tstate.popSum(tau);
					tstate.setCurrentPrefix(tau);
					tstate.printInfo();
					
					log.info("timeout expired, throwing a TimeExpiredException");
					
					// get the exception ClassInfo
					ClassInfo ci = ClassInfo.getInitializedClassInfo(TimeExpiredException.class.getName(), ti);
					
					// create the new exception and push on the top stack
					StackFrame sf = new JVMStackFrame(insn.getMethodInfo());
					sf.push(ti.getHeap().newObject(ci, ti).getObjectRef());
					ti.pushFrame(sf);
					
					ATHROW athrow = new ATHROW();
					
					//schedule the next instruction
					ti.skipInstruction(athrow);
					return;
				}
				else {
					String action = actions.get(choice);
					String value = listener.getActionValue(contractID, action);

					log.info("returning message: ["+action+":"+value+"]");
					
					DoReceiveDS p = new DoReceiveDS(); 
					p.session = sessionID;
					p.action = action;
					
					sum.prefixes.add(p);
					
					log.info("setting current prefix: "+p);
					tstate.popSum(p);
					tstate.setCurrentPrefix(p);
					tstate.printInfo();
					
					ElementInfo message = getMessage(ti, action, value, sessionID);
					
					StackFrame frame = ti.getTopFrame();
					frame.setReferenceResult(message.getObjectRef(), null);
					
					Instruction nextInsn = new ARETURN();
					nextInsn.setMethodInfo(insn.getMethodInfo());
					
					ti.skipInstruction(nextInsn);
					return;
				}
			}
		}
		else {
			log.info("single choice");
			
			String action = actions.get(0);
			String value = listener.getActionValue(contractID, action);

			DoReceiveDS p = new DoReceiveDS(); 
			p.session = sessionID;
			p.action = action;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(p);
			
			log.info("returning message: ["+action+":"+value+"]");
			
			log.info("setting current process: "+sum);
			log.info("setting current prefix: "+p);
			tstate.setCurrentProcess(sum);		//set the current process
			tstate.setCurrentPrefix(p);
			tstate.printInfo();
			
			//build the return value
			ElementInfo messageEI = getMessage(ti, action, value, sessionID);
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(messageEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
		
	}

}
