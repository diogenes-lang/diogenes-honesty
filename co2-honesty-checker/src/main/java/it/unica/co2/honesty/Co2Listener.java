package it.unica.co2.honesty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import co2api.CO2ServerConnection;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.IntChoiceFromList;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.contract.utils.ContractExplorer;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.dto.CO2DataStructures.AskDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoReceiveDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoSendDS;
import it.unica.co2.honesty.dto.CO2DataStructures.IfThenElseDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ParallelProcessesDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.RetractDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;
import it.unica.co2.util.ObjectUtils;

public class Co2Listener extends ListenerAdapter {
	
	private static Logger log = JPF.getLogger(Co2Listener.class.getName());
	
	public Co2Listener(Config conf, Class<? extends Participant> processClass) {
		
		if (conf.getBoolean("honesty.listener.log", false)) {
			log.setLevel(Level.ALL);
			ThreadState.logger.setLevel(Level.ALL);
		}
		else {
			log.setLevel(Level.OFF);
			ThreadState.logger.setLevel(Level.OFF);
		}
		
		
		this.processUnderTestClass = processClass;
	}
	
	/*
	 * the class of the project to analyze
	 */
	private final Class<? extends Participant> processUnderTestClass;
	
	/*
	 * all told contracts
	 */
	private Map<String, ContractDefinition> contracts = new TreeMap<>();
	
	/*
	 * all sessions of the process under test
	 */
	private List<String> sessions = new ArrayList<>();
	
	/*
	 * env
	 */
	private Map<String, ProcessDefinitionDS> envProcesses = new HashMap<>();
	private List<ProcessDefinitionDS> envProcessesList  = new ArrayList<>();
	
	private Map<ThreadInfo,ThreadState> threadStates = new HashMap<>();
	private ThreadInfo mainThread;
	
	/*
	 * entry-point of the next thread.
	 * A thread starts another thread at time (using synchronized Participant#parallel()).
	 * Until parallel() does't end, no other one can start a new thread.
	 */
	private SumDS threadCurrentProcess;
	private PrefixPlaceholderDS threadCurrentPrefix;
	
	/* using these fields is more efficient on dispatching */
	private MethodInfo Participant_tell;
	private MethodInfo Participant_setConnection;
	private MethodInfo CO2Process_parallel;
	private MethodInfo CO2Process_processCall;
	private MethodInfo Public_waitForSession;
	private MethodInfo Public_waitForSessionT;
	private MethodInfo Session2_waitForReceive;
	private MethodInfo Session2_send;
	private MethodInfo Session2_sendString;
	private MethodInfo Session2_sendInt;
	private MethodInfo Message_getStringValue;
	private MethodInfo TST_setFromString;
	
	// collect the 'run' methods in order to avoid re-build of an already visited CO2 process
	private HashSet<MethodInfo> methodsToSkip = new HashSet<>();
	
	
	private Map<String, Boolean> contractsDelay = new HashMap<>();
	private Map<String, Integer> contractsCount = new HashMap<>();
	private Map<String, String> publicSessionName = new HashMap<>();
	private int sessionCount = 0;
	
	@Override
	public void classLoaded(VM vm, ClassInfo ci) {

		if (ci.getName().equals(Participant.class.getName())) {
			if (Participant_tell == null)
				Participant_tell = ci.getMethod("_tell", "(Lit/unica/co2/api/contract/ContractDefinition;Ljava/lang/String;Lco2api/Private;Ljava/lang/Integer;)Lco2api/Public;", false); 
			
			if (Participant_setConnection==null)
				Participant_setConnection = ci.getMethod("setConnection", "()V", false);
		}
		
		if (ci.getName().equals(CO2Process.class.getName())) {
			if (CO2Process_parallel == null)
				CO2Process_parallel = ci.getMethod("parallel", "(Ljava/lang/Runnable;)J", false); 
			
			if (CO2Process_processCall == null)
				CO2Process_processCall = ci.getMethod("processCall", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V", false);
		}
		
		if (ci.getName().equals(Public.class.getName())) {
			
			if (Public_waitForSession==null)
				Public_waitForSession = ci.getMethod("waitForSession", "()Lco2api/Session;", false);
			
			if (Public_waitForSessionT==null)
				Public_waitForSessionT = ci.getMethod("waitForSession", "(Ljava/lang/Integer;)Lco2api/Session;", false);
		}

		if (ci.getName().equals(Session2.class.getName())) {
			
			if (Session2_waitForReceive==null)
				Session2_waitForReceive = ci.getMethod("waitForReceive", "(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;", false);
			
			if (Session2_send==null)
				Session2_send = ci.getMethod("send", "(Ljava/lang/String;)Ljava/lang/Boolean;", false);
			
			if (Session2_sendString==null)
				Session2_sendString = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Boolean;", false);
			
			if (Session2_sendInt==null)
				Session2_sendInt = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/Boolean;", false);
		}
		
		if (ci.getName().equals(Message.class.getName())) {
			
			if (Message_getStringValue==null)
				Message_getStringValue = ci.getMethod("getStringValue", "()Ljava/lang/String;", false);
		}
		
		if (ci.getName().equals(TST.class.getName())) {
			
			if (TST_setFromString==null)
				methodsToSkip.add(ci.getMethod("setFromString", "(Ljava/lang/String;)V", false));
		}
	}
	
	@Override
	public void instructionExecuted (VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn) {
	
	}
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ThreadState tstate = threadStates.get(ti);

//		ClassInfo ci = ti.getExecutingClassInfo();
//		if (ci.getName().equals(processUnderTestClass.getName())) {
//			log.info("*** "+insnToString(insn));
//		}
		
		if(Participant_tell!=null && insn==Participant_tell.getFirstInsn()) {
			handleTell(tstate, ti, insn);
		}
		else if(Public_waitForSession!=null && insn==Public_waitForSession.getFirstInsn()) {
			handleWaitForSession(tstate, ti, insn, false);
		}
		else if(Public_waitForSessionT!=null && insn==Public_waitForSessionT.getFirstInsn()) {
			handleWaitForSession(tstate, ti, insn, true);
		}
		else if(Session2_waitForReceive!=null && insn==Session2_waitForReceive.getFirstInsn()) {
			handleWaitForReceive(tstate, ti, insn);
		}
		else if(Message_getStringValue!=null && insn==Message_getStringValue.getFirstInsn()) {
			handleMessageGetStringValue(ti, insn);
		}
		else if (methodsToSkip.contains(insn.getMethodInfo()) && insn == insn.getMethodInfo().getFirstInsn()) {
			handleSkipRunMethod(ti, insn);
		}
		else if (insn instanceof SwitchInstruction && tstate.considerSwitchInstruction((SwitchInstruction) insn)) {
			tstate.setSwitchInsn((SwitchInstruction) insn);
		}
		else if (insn instanceof IfInstruction && tstate.considerIfInstruction((IfInstruction) insn)) {
			handleIfThenElse(tstate, ti, insn);
		}
		else if (
				(Session2_send!=null && insn==Session2_send.getFirstInsn()) ||
				(Session2_sendInt!=null && insn==Session2_sendInt.getFirstInsn()) ||
				(Session2_sendString!=null && insn==Session2_sendString.getFirstInsn())
				) {
			handleSession2Send(tstate, ti, insn);
		}
		else if(Participant_setConnection!=null && insn==Participant_setConnection.getFirstInsn()) {
			handleParticipantSetConnection(ti, insn);
		}
	}
	

	private void handleParticipantSetConnection(ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("HANDLE -> PARTICIPANT SET CONNECTION");
		
		//object Participant
		ElementInfo participant = ti.getThisElementInfo();
		
		ClassInfo connectionCI = ClassInfo.getInitializedClassInfo(CO2ServerConnection.class.getName(), ti);
		ElementInfo connectionEI = ti.getHeap().newObject(connectionCI, ti);
		
		participant.setReferenceField("connection", connectionEI.getObjectRef());
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(connectionEI.getObjectRef(), null);
		
		Instruction nextInsn = new RETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}


	private void handleSession2Send(ThreadState tstate, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("-- SEND --");
		
		/*
		 * collect the co2 process
		 */
		ElementInfo session2 = ti.getThisElementInfo();	//the class that call waitForReceive(String...)
		
		String sessionName = getSessionName(ti, session2.getReferenceField("contract"));
		String action = getArgumentString(ti, 0);
		
		DoSendDS send = new DoSendDS();
		send.session = sessionName;
		send.action = action;
		
		if (insn.getMethodInfo()==Session2_send) send.sort = Sort.UNIT;
		if (insn.getMethodInfo()==Session2_sendInt) send.sort = Sort.INT;
		if (insn.getMethodInfo()==Session2_sendString) send.sort = Sort.STRING;
		
		SumDS sum = new SumDS();
		sum.prefixes.add(send);
		
		log.info("setting current process: "+sum);
		
		tstate.setCurrentProcess(sum);		//set the current process
		tstate.setCurrentPrefix(send);		//set the current prefix

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
	
	
	private String getSessionName(ThreadInfo ti, int objReference) {
		ElementInfo pbl = ti.getElementInfo(objReference);
		assert pbl!=null;
		
		String sessionName = publicSessionName.get(pbl.toString());
		assert sessionName!=null;
		
		return sessionName;
	}
	
	
	private void handleIfThenElse(ThreadState tstate, ThreadInfo ti, Instruction insn) {

		IfInstruction ifInsn = (IfInstruction) insn;
		
		if (!ti.isFirstStepInsn()) { // top half - first execution

			log.finer("TOP HALF");
			
			log.finer("");
			tstate.printInfo();
			log.finer("--IF_THEN_ELSE--");
			
			IfThenElseDS ifThenElse = new IfThenElseDS();
			ifThenElse.thenStmt = new PrefixPlaceholderDS();
			ifThenElse.elseStmt = new PrefixPlaceholderDS();
			
			
			tstate.setCurrentProcess(ifThenElse);		//set the current process
			tstate.pushIfElse(ifThenElse);

			BooleanChoiceGenerator cg = new BooleanChoiceGenerator(tstate.getIfThenElseChoiceGeneratorName(), false);

			boolean cgSetOk = ti.getVM().getSystemState().setNextChoiceGenerator(cg);
			
			assert cgSetOk : "error setting the choice generator";
			
			ti.skipInstruction(insn);
			log.finer("re-executing: "+insnToString(insn));
		}
		else {

			log.finer("BOTTOM HALF");
			
			// bottom half - reexecution at the beginning of the next
			// transition
			BooleanChoiceGenerator cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(tstate.getIfThenElseChoiceGeneratorName(), BooleanChoiceGenerator.class);

			assert cg != null : "no 'ifThenElseCG' BooleanChoiceGenerator found";
			
			
			ifInsn.popConditionValue(ti.getModifiableTopFrame());		//remove operands from the stack
			
			Boolean myChoice = cg.getNextChoice();
			
			
			PrefixPlaceholderDS thenTau = tstate.getThenPlaceholder();
			PrefixPlaceholderDS elseTau = tstate.getElsePlaceholder();
			
			log.finer("thenTau: "+thenTau);
			log.finer("elseTau: "+elseTau);
			
			
			if (myChoice){
				/*
				 * then branch
				 */
				log.finer("THEN branch, setting tau, choice: "+myChoice);
				log.finer("next insn: "+ifInsn.getNext().getPosition());
				
				tstate.setCurrentPrefix(thenTau);		//set the current prefix

				ti.skipInstruction(ifInsn.getNext());
				
				tstate.setPeekThen();
				tstate.popIfElse();
			}
			else {
				/*
				 * else branch
				 */
				log.finer("ELSE branch, setting tau, choice: "+myChoice);
				log.finer("next insn: "+ifInsn.getTarget().getPosition());

				tstate.setCurrentPrefix(elseTau);		//set the current prefix
				
				ti.skipInstruction(ifInsn.getTarget());
				
				tstate.setPeekElse();
				tstate.popIfElse();
			}
			
			tstate.printInfo(Level.FINER);
		}
	}


	private void handleSkipRunMethod(ThreadInfo ti, Instruction insn) {
		log.info("");
		log.info("SKIPPING METHOD: "+insn.getMethodInfo().getFullName());
		
		// it works only for methods that return VOID
		assert insn.getMethodInfo().getReturnTypeCode()==Types.T_VOID;
		
		Instruction nextInsn = new RETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}


	private void handleMessageGetStringValue(ThreadInfo ti, Instruction insn) {
		//bypass the type check of the message
		
		ElementInfo message = ti.getThisElementInfo();
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(message.getReferenceField("stringVal"), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}


	private void handleWaitForReceive(ThreadState ts, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("-- WAIT FOR RECEIVE --");
		
		ElementInfo session2 = ti.getThisElementInfo();	//the class that call waitForReceive(String...)
		
		String sessionName = getSessionName(ti, session2.getReferenceField("contract"));
		
		//parameters
		boolean timeout = getArgumentInteger(ti, 0)>0;
		List<String> actions = getArgumentStringArray(ti, 1);
		
		assert actions.size()>=1 : "you must pass at least one action";
		
		log.info("timeout: "+timeout);
		log.info("actions: "+actions);
		
		if (timeout || actions.size()>1) {
			log.info("considering multiple choices");
			
			
			if (!ti.isFirstStepInsn()) {
				
				log.info("TOP-HALF");
				
				SumDS sum = new SumDS();
				
				log.info("pushing the sum: "+sum);
				
				if (timeout) {
					String[] arr = new String[actions.size()+1];
					arr[0] = "t";
					actions.stream().forEach((x)->{arr[actions.indexOf(x)+1]=x;});
					ts.pushSum(sum, arr);		//set the current process
				}
				else
					ts.pushSum(sum, actions.toArray(new String[]{}));
					
				ts.setCurrentProcess(sum);
				
				List<Integer> choiceSet = new ArrayList<>();
				
				actions.stream().forEach((x)-> {choiceSet.add(actions.indexOf(x));});
				if (timeout) choiceSet.add(actions.size());
				
				assert choiceSet.size()>1;

				IntChoiceFromList cg = new IntChoiceFromList(ts.getWaitForReceiveChoiceGeneratorName(), choiceSet.stream().mapToInt(i -> i).toArray());
				ti.getVM().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				return;
			}
			else {
				
				log.info("BOTTOM-HALF");
				
				SumDS sum = ts.getSum();
				
				// get the choice generator
				IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(ts.getWaitForReceiveChoiceGeneratorName(), IntChoiceFromList.class);
				
				// take a choice
				
				int choice = cg.getNextChoice();
				
				if (choice==actions.size()) {
					
					log.info("timeout expired");
					
					TauDS tau = new TauDS();
					sum.prefixes.add(tau);
					
					log.info("setting current prefix: "+tau);
					ts.popSum(tau);
					ts.setCurrentPrefix(tau);
					
					log.info("timeout expired, throwing a TimeExpiredException");
					
					// get the exception ClassInfo
					ClassInfo ci = ClassInfo.getInitializedClassInfo(TimeExpiredException.class.getName(), ti);
					
					// create the new exception and push on the top stack
					ti.getModifiableTopFrame().push(ti.getHeap().newObject(ci, ti).getObjectRef());
					ATHROW athrow = new ATHROW();
					
					//schedule the next instruction
					ti.skipInstruction(athrow);
					return;
				}
				else {
					String action = actions.get(choice);
					String value = "10";
					
					log.info("returning message: ["+action+":"+value+"]");
					
					DoReceiveDS p = new DoReceiveDS(); 
					p.session = sessionName;
					p.action = action;
					
					sum.prefixes.add(p);
					
					log.info("setting current prefix: "+p);
					ts.popSum(p);
					ts.setCurrentPrefix(p);
					
					ElementInfo message = getMessage(ti, action, value);
					
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
			
			DoReceiveDS p = new DoReceiveDS(); 
			p.session = sessionName;
			p.action = action;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(p);
			
			log.info("setting current process: "+sum);
			log.info("setting current prefix: "+p);
			ts.setCurrentProcess(sum);		//set the current process
			ts.setCurrentPrefix(p);
			
			
			log.info("returning a new Message");
			
			//build the return value
			ElementInfo messageEI = getMessage(ti, action, "1");
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(messageEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
		
		
	}

	
	private ElementInfo getMessage(ThreadInfo ti, String label, String value) {
		
		ClassInfo messageCI = ClassInfo.getInitializedClassInfo(Message.class.getName(), ti);
		ElementInfo messageEI = ti.getHeap().newObject(messageCI, ti);
		
		messageEI.setReferenceField("label", ti.getHeap().newString(label, ti).getObjectRef());
		messageEI.setReferenceField("stringVal", ti.getHeap().newString(value, ti).getObjectRef());
		
		return messageEI;
	}

	
	private void handleWaitForSession(ThreadState tstate, ThreadInfo ti, Instruction insn, boolean hasTimeout) {
		log.info("");
		log.info("-- WAIT FOR SESSION --");
		
		//object Public
		ElementInfo pbl = ti.getThisElementInfo();
		
		String sessionName = getSessionName(ti, pbl.getObjectRef());
		
		String contractID = pbl.getStringField("uniqueID");
		log.info("contractID: "+contractID);
		
		boolean hasDelay = contractsDelay.get(contractID);
		
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
					
					AskDS ask = new AskDS();
					ask.session = sessionName;
					sum.prefixes.add(ask);
					
					log.info("setting current prefix: "+ask);
					tstate.setCurrentPrefix(ask);
					tstate.popSum(ask);

					//build the return value
					ClassInfo sessionCI = ClassInfo.getInitializedClassInfo(Session.class.getName(), ti);
					ElementInfo sessionEI = ti.getHeap().newObject(sessionCI, ti);
					
					sessionEI.setReferenceField("connection", pbl.getReferenceField("connection"));
					sessionEI.setReferenceField("contract", pbl.getObjectRef());
					
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
					retract.session = sessionName;
					sum.prefixes.add(retract);
					
					log.info("setting current prefix: "+retract);
					tstate.setCurrentPrefix(retract);
					tstate.popSum(retract);
					
					log.info("delay expired, throwing a ContractExpiredException");
					
					// get the exception ClassInfo
					ClassInfo ci = ClassInfo.getInitializedClassInfo(ContractExpiredException.class.getName(), ti);
					
					// create the new exception and push on the top stack
					ti.getModifiableTopFrame().push(ti.getHeap().newObject(ci, ti).getObjectRef());
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

			AskDS ask = new AskDS();
			ask.session = sessionName;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(ask);
			
			log.info("setting current process: "+sum);
			log.info("setting current prefix: "+ask);
			tstate.setCurrentProcess(sum);		//set the current process
			tstate.setCurrentPrefix(ask);
			
			log.info("returning a new Session");
			
			//build the return value
			ClassInfo sessionCI = ClassInfo.getInitializedClassInfo(Session.class.getName(), ti);
			ElementInfo sessionEI = ti.getHeap().newObject(sessionCI, ti);
			
			sessionEI.setReferenceField("connection", pbl.getReferenceField("connection"));
			sessionEI.setReferenceField("contract", pbl.getObjectRef());
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(sessionEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
		
	}

	
	private void handleTell(ThreadState tstate, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("HANDLE -> TELL");

		//parameters
		String cserial = getArgumentString(ti, 1);
		ElementInfo pvt = getArgumentElementInfo(ti, 2);
		int delay = getArgumentInteger(ti, 3);
		
		log.info("delay: "+delay);
		
		//get private fields (in order to build the public object)
		ElementInfo connection = ti.getElementInfo(pvt.getReferenceField("connection"));
		ElementInfo contract = ti.getElementInfo(pvt.getReferenceField("contract"));
		
		//build a unique ID for the contract (in order to handle delays appropriately when waitForSession is invoked)
		String contractID = cserial;

		Integer contractCount = contractsCount.get(contractID);
		contractCount = contractCount==null? 0 : contractCount+1;
		
		contractsCount.put(contractID, contractCount);
		contractID = "n"+contractCount+"_"+contractID;
		contractsDelay.put(contractID, delay>0);
		
		
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
		
		String sessionName = "x"+sessionCount;
		sessionCount++;
		
		log.info("storing <"+pblEI.toString()+","+sessionName+">");
		if (!publicSessionName.containsKey(pblEI.toString()))
			publicSessionName.put(pblEI.toString(), sessionName);
		
		
		
		
		TellDS tell = new TellDS();
		SumDS sum = new SumDS(tell);
		
		ContractDefinition cDef = ObjectUtils.deserializeObjectFromStringQuietly(cserial, ContractDefinition.class);
		contracts.put(cDef.getName(), cDef);
		
		ContractExplorer.findAll(
				cDef.getContract(), 
				ContractReference.class,
				(x)->(x.getReference()!=cDef),
				(x)->{
					contracts.put(x.getReference().getName(), x.getReference());
				}
			);
		
		sessions.add(sessionName);
		
		tell.contractName = cDef.getName();
		tell.session = sessionName;
		
		tstate.setCurrentProcess(sum);		//set the current process
		tstate.setCurrentPrefix(tell);

		tstate.printInfo();
	}


	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());

		if (
				exitedMethod.getName().equals("run") &&
				envProcesses.containsKey(ci.getSimpleName())
				) {
			/*
			 * the process is finished
			 */
			log.info("");
			tstate.printInfo();
			log.info("--RUN ENV PROCESS-- (method exited) -> "+ci.getSimpleName());
			
			tstate.tryToPopFrame();
			
			//next flag prevent from re-build the process at each invocation
			envProcesses.get(ci.getSimpleName()).alreadyBuilt = true;
			
			tstate.printInfo();
		}
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());
		
		if (enteredMethod==CO2Process_parallel) {
			log.info("");
			log.info("--PARALLEL-- (method entered) -> ID:"+tstate.getId());
			
			ParallelProcessesDS parallel = new ParallelProcessesDS();
			
			SumDS sumA = new SumDS();
			PrefixPlaceholderDS tauA = new PrefixPlaceholderDS();
			sumA.prefixes.add(tauA);
			
			SumDS sumB = new SumDS();
			PrefixPlaceholderDS tauB = new PrefixPlaceholderDS();
			sumB.prefixes.add(tauB);
			
			parallel.processA = sumA;
			parallel.processB = sumB;
			
			tstate.setCurrentProcess( parallel);
			tstate.setCurrentPrefix( tauB);
			
			threadCurrentProcess = sumA;
			threadCurrentPrefix = tauA;
		}
		else if (enteredMethod==CO2Process_processCall) {
			log.info("");
			tstate.printInfo();
			log.info("--PROCESS CALL-- (method entered) -> "+ci.getSimpleName());
			
			String className = getArgumentString(currentThread, 1);			// the classname of the process that we want to invoke
			List<ElementInfo> args = getArgumentArray(currentThread, 2);
			
			if (envProcesses.containsKey(className)) {
				log.info("envProcess "+className+" already exists");
			}
			else {
				/*
				 * instantiate a new env process
				 */
				ProcessDefinitionDS proc = new ProcessDefinitionDS();
				proc.name = className;
				proc.firstPrefix = new PrefixPlaceholderDS();
				proc.process = new SumDS(proc.firstPrefix);
				
//				List<ElementInfo> args = getAllArgumentsAsElementInfo(currentThread);
				
				if (args.size()==0) {
					//add at least one argument to make the process valid
					proc.freeNames.add("exp");
				}
				
				for (ElementInfo ei : args) {
					if (ei.getClassInfo().getName().equals(Session2.class.getName())) {
						ElementInfo pbl = currentThread.getElementInfo(ei.getReferenceField("contract"));
						assert pbl!=null;
						String sessionName = publicSessionName.get(pbl.toString());
						
						proc.freeNames.add("\""+sessionName+"\"");
						log.info("ctor arg: Session2 ("+sessionName+")");
					}
					else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
						proc.freeNames.add("exp");
						log.info("ctor arg: Number");
					}
					else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
						proc.freeNames.add("exp");
						log.info("ctor arg: String");
					}
				}
				
				// store the process for future retrieve (when another one1 call it)
				log.info("saving envProcess "+className);
				envProcesses.put(className, proc);
				envProcessesList.add(proc);
			}
			
			
			
			ProcessCallDS pCall = new ProcessCallDS();

			pCall.name = className;
			log.info("processName: "+pCall.name);
			
			for (ElementInfo ei : getArgumentArray(currentThread, 2)) {
				
				if (ei.getClassInfo().getName().equals(Session2.class.getName())) {
					ElementInfo pbl = currentThread.getElementInfo(ei.getReferenceField("contract"));
					assert pbl!=null;
					String sessionName = publicSessionName.get(pbl.toString());
					
					pCall.params.add("\""+sessionName+"\"");
					log.info("param: Session2 ("+sessionName+")");
				}
				else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
					pCall.params.add("exp");
					log.info("param: Number");
				}
				else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
					pCall.params.add("exp");
					log.info("param: String");
				}
			}
			
			tstate.setCurrentProcess(pCall);
			tstate.setCurrentPrefix(null);
		}
		else if (	enteredMethod.getName().equals("run") &&
					envProcesses.containsKey(ci.getSimpleName())
					) {
			/*
			 * the process is finished
			 */
			log.info("");
			tstate.printInfo();
			log.info("--RUN ENV PROCESS-- (method entered) -> "+ci.getSimpleName());
			
			/*
			 * Check for recursive behavior
			 */
			ProcessDefinitionDS proc = envProcesses.get(ci.getSimpleName());

			boolean recursiveCall = tstate.checkForRecursion(proc);
			
			if (recursiveCall || proc.alreadyBuilt) {
				
				if (recursiveCall) {
					// the call is recursive: stop search
					log.info("recursive call detected, terminating");
				}
				
				if (proc.alreadyBuilt) {
					// the process was already called
					// the flag is set when called process returns (exit of the 'run' method)
					log.info("process already built: "+proc.toString());
				}
				
				methodsToSkip.add(enteredMethod);
			}
			else {
				log.info("NOT recursive call AND NOT already built");
			}
			
			log.info("adding a new process onto the stack: "+proc.toString());
			tstate.pushNewFrame(proc);
			tstate.printInfo();
		}
	}
	
	
	
	@Override
	public void objectCreated(VM vm, ThreadInfo currentThread, ElementInfo newObject) {
		
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		log.info("----------------NEW---------------: "+newCG.getId()+" - idRef="+newCG.getIdRef());
	}
	
	@Override
	public void stateBacktracked(Search search) {
		log.info("<<<<<<<<<< BACKTRACK <<<<<<<<<<");
	}

	@Override
	public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> processedCG) {
		log.info("............... PROCESSED ..............: "+processedCG.getId()+" - idRef="+processedCG.getIdRef());
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		log.info(">>>>>>>>>> ADVANCE >>>>>>>>>>: "+currentCG.getId()+" - idRef="+currentCG.getIdRef());
	}
	
	@Override
	public void searchStarted(Search search) {
		log.info("");
		log.info("vvvvvvvvvvvvvvvvv SEARCH STARTED vvvvvvvvvvvvvvvvv");
		log.info("thread ID: "+search.getVM().getCurrentThread().getId());
		
		ThreadInfo tInfo = search.getVM().getCurrentThread();
		ThreadState tState = new ThreadState(tInfo);
		
		threadStates.put(tInfo, tState);
		
		mainThread = tInfo;
	}
	
	
	@Override
	public void searchFinished(Search search) {
		log.info("");
		log.info("vvvvvvvvvvvvvvvvv SEARCH FINISHED vvvvvvvvvvvvvvvvv");
		
		log.info("contracts:");
		for (Entry<String, ContractDefinition> entry : contracts.entrySet()) {
			log.info("\t"+entry.getKey()+" --> "+entry.getValue().getContract().toMaude());
		}
		
		log.info("env processes:");
		for (Entry<String, ProcessDefinitionDS> entry : envProcesses.entrySet()) {
			log.info("\t"+entry.getValue().toString());
		}
		
		log.info("sessions: "+sessions);
	}
	
	@Override
	public void threadStarted(VM vm, ThreadInfo startedThread) {
		
		log.info("===================== THREAD STARTED =====================");
		log.info("thread ID: "+startedThread.getId());
		
		ThreadState tState = new ThreadState(startedThread);
		
		assert threadCurrentProcess!=null;
		assert threadCurrentPrefix!=null;
		
		tState.setCurrentProcess(threadCurrentProcess);
		tState.setCurrentPrefix(threadCurrentPrefix);
		
		threadStates.put(startedThread, tState);
	}
	

//	@SuppressWarnings("unused")
	private String insnToString(Instruction insn) {
		return insn.getPosition() + " - " + insn.getMnemonic() + " ("+insn.getMethodInfo().getFullName()+")";
	}
	
	/**
	 * Get the actual parameters of the given thread
	 * @param currentThread
	 * @return
	 */
	private Object[] getArguments(ThreadInfo currentThread) {
		return currentThread.getTopFrame().getArgumentValues(currentThread);
	}
	
	private ElementInfo getArgumentElementInfo(ThreadInfo currentThread, int position) {
		ElementInfo eiArgument = (ElementInfo) getArguments(currentThread)[position];
		return eiArgument;
	}
	
	private String getArgumentString(ThreadInfo currentThread, int position) {
		return getArgumentElementInfo(currentThread, position).asString();
	}
	
	private Integer getArgumentInteger(ThreadInfo currentThread, int position) {
		return (Integer) getArgumentElementInfo(currentThread, position).asBoxObject();
	}
	
	private List<ElementInfo> getArgumentArray(ThreadInfo currentThread, int position) {
		List<ElementInfo> elms = new ArrayList<>();
		
		ArrayFields af = getArgumentElementInfo(currentThread, position).getArrayFields();
		
		//iterate and collect the elements
		for (int i=0; i<af.arrayLength(); i++) {
			int ref = af.getReferenceValue(i);
			ElementInfo s = currentThread.getElementInfo(ref);
			
			elms.add(s);
		}
		
		return elms;
	}
	
	private List<String> getArgumentStringArray(ThreadInfo currentThread, int position) {
		
		List<String> strings = new ArrayList<String>();
		
		for (ElementInfo ei : getArgumentArray(currentThread, position)) {
			strings.add(ei.asString());
		}
		
		return strings;
	}
	
	@SuppressWarnings("unused")
	private List<ElementInfo> getAllArgumentsAsElementInfo(ThreadInfo currentThread) {
		List<ElementInfo> args = new ArrayList<ElementInfo>();
		
		for (Object obj : getArguments(currentThread)) {
			args.add((ElementInfo) obj);
		}
		
		return args;
	}
	
	
	
	
	
	
	
	
	//--------------------------------- GETTERS and SETTERS -------------------------------
	public Class<? extends Participant> getProcessUnderTestClass() {
		return processUnderTestClass;
	}
	
	public ProcessDS getCo2Process() {
		return threadStates.get(mainThread).getFirstProcess();
	}
	
	public Map<String, ContractDefinition> getContracts() {
		return contracts;
	}
	
	public List<String> getSessions() {
		return sessions;
	}
	
	public Collection<ProcessDefinitionDS> getEnvProcesses() {
		return envProcessesList;
	}
	
	public Collection<String> getEnvProcessesNames() {
		List<String> tmp = new ArrayList<>();
		
		for (ProcessDefinitionDS p : envProcessesList) {
			tmp.add(p.name);
		}
		
		return tmp;
	}
	
	
	
}