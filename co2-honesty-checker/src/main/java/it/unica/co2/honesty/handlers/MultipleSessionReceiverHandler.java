package it.unica.co2.honesty.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co2api.ContractModel;
import co2api.SessionI;
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
import it.unica.co2.util.ObjectUtils;


class MultipleSessionReceiverHandler extends InstructionHandler {
	
	@SuppressWarnings("unchecked")
	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("-- WAIT FOR RECEIVE (multi session) -- ");

		
		//parameters
		boolean timeout = listener.getArgumentInt(ti, 0)>0;
		log.info("timeout: "+timeout);
		
		ElementInfo mReceiver = ti.getThisElementInfo();
		
		// get the serialized map
		String serializedSessionActionsMap = mReceiver.getStringField("serializedSessionActionsMap");
		
		
		List<String> choices = new ArrayList<>();
		Map<String, SessionI<? extends ContractModel>> sessionIDMap = new HashMap<>();
		Map<SessionI<? extends ContractModel>, List<String>> sessionActionsMap;
		
		
		sessionActionsMap = ObjectUtils.deserializeObjectFromStringQuietly(serializedSessionActionsMap, Map.class);
		
		/*
		 * each session contains a map of <action, consumer-idx>
		 * the consumer-idx allows to retrieve the consumer from the corresponding array
		 */
		choices = new ArrayList<>();
		
		List<SessionI<? extends ContractModel>> sessions = new ArrayList<>();
		sessions.addAll(sessionActionsMap.keySet());
		sessions.sort(
				(x,y) -> {
					String xName = listener.getSessionID(x.getPublicContract().getUniqueID());
					String yName = listener.getSessionID(y.getPublicContract().getUniqueID()); 
					return xName.compareTo(yName);
				});
		
		for (SessionI<? extends ContractModel> session :  sessions) {
			
			List<String> actions = sessionActionsMap.get(session);
			
			String sessionID = listener.getSessionID(session.getPublicContract().getUniqueID());
			
			sessionIDMap.put(sessionID, session);
			
			for (String action : actions)
				choices.add(sessionID+"$"+action);
		}
		
		
		log.info("choices: "+choices);
		
		
		
		if (!ti.isFirstStepInsn()) { // top half - first execution
		
			log.info("TOP HALF");
			

			/*
			 * choice generation
			 */
			
			SumDS sum = new SumDS();
			
			final String[] sumPrefixes;
			if (timeout) {
				sumPrefixes = new String[choices.size()+1];
				sumPrefixes[0] = "t";
				
				for (String choice : choices) {
					sumPrefixes[choices.indexOf(choice)+1]=choice;
				}
			}
			else
				sumPrefixes= choices.toArray(new String[]{});
			
			tstate.pushSum(sum, sumPrefixes);		//set the current process
			tstate.setCurrentProcess(sum);
			tstate.printInfo();
			
			List<Integer> choiceSet = new ArrayList<>();
			
			for (String choice : choices) {
				choiceSet.add(choices.indexOf(choice));
			}
			if (timeout) choiceSet.add(choices.size());
			
			assert choiceSet.size()>1;
			assert choiceSet.size()==choices.size() || choiceSet.size()-1==choices.size();

			log.info("sumPrefixies: "+Arrays.toString(sumPrefixes));
			log.info("choiceSet: "+choiceSet);
			
			IntChoiceFromList cg = new IntChoiceFromList(tstate.getWaitForMultipleReceiveChoiceGeneratorName(), choiceSet.stream().mapToInt(i -> i).toArray());
			ti.getVM().setNextChoiceGenerator(cg);
			ti.skipInstruction(insn);
			return;
		
		}
		else {
		
			log.info("BOTTOM HALF");
			log.info(">>> "+Co2Listener.insnToString(insn));
			
			log.info("choices: "+choices);
			log.info("choices: "+choices.size());
			
			assert choices.size()>0;
			
			SumDS sum = tstate.getSum();
			
			// get the choice generator
			IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(tstate.getWaitForMultipleReceiveChoiceGeneratorName(), IntChoiceFromList.class);
			
			assert cg!=null : "choice generator not found: "+tstate.getWaitForMultipleReceiveChoiceGeneratorName();
			
			// take a choice
			int choice = cg.getNextChoice();
			log.info("choice: "+choice+"/"+cg.getTotalNumberOfChoices());
			
			if (choice==choices.size()) {
				
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
				
				// get sessionID and action related to this choice
				log.info("choice: "+choices.get(choice));
				
				String[] tmp = choices.get(choice).split("\\$");
				String sessionID = tmp[0];	
				String action = tmp[1];
				
				// create a message that will be passed to the consumer
				String contractID = sessionIDMap.get(sessionID).getPublicContract().getUniqueID();
				String value = listener.getActionValue(contractID, action);
				
				ElementInfo message = getMessage(ti, action, value, getSession(ti, contractID, getPublic(ti, contractID)));
				
				// set the corresponding CO2 data
				log.info("returning message: ["+action+":"+value+"]");
				
				DoReceiveDS p = new DoReceiveDS(); 
				p.session = sessionID;
				p.action = action;
				
				sum.prefixes.add(p);
				
				log.info("setting current prefix: "+p);
				tstate.popSum(sessionID+"$"+action);
				tstate.setCurrentPrefix(p);
				tstate.printInfo();
				
				// create a new frame with the message
				StackFrame frame = ti.getTopFrame();
				frame.setReferenceResult(message.getObjectRef(), null);
				
				Instruction nextInsn = new ARETURN();
				nextInsn.setMethodInfo(insn.getMethodInfo());
				
				ti.skipInstruction(nextInsn);
				
			}
		}
	}
	
}
