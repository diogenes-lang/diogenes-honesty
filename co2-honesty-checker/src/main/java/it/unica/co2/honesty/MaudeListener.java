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

import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ExceptionInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
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
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.RetractDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;
import it.unica.co2.util.ObjectUtils;

public class MaudeListener extends ListenerAdapter {
	
	private static Logger log = JPF.getLogger(MaudeListener.class.getName());
	
	public MaudeListener(Config conf, Class<? extends Participant> processClass) {
		
		if (conf.getBoolean("honesty.listener.log", false)) {
			log.setLevel(Level.INFO);
			ThreadState.logger.setLevel(Level.INFO);
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
	private MethodInfo participantTell;
	private MethodInfo participantWaitForSession;
	private MethodInfo sessionWaitForReceive;
	private MethodInfo sessionSend;
	private MethodInfo sessionSendString;
	private MethodInfo sessionSendInt;
	private MethodInfo parallel;
	private MethodInfo processCall;
	private MethodInfo ifThenElse;

	private MethodInfo test;
	private MethodInfo privateTell;
	private MethodInfo waitForSession;
	private MethodInfo waitForSessionT;
	private MethodInfo waitForReceive;
	private MethodInfo messageGetStringValue;
	
	// collect the 'run' methods in order to avoid re-build of an already visited CO2 process
	private HashSet<MethodInfo> methodsToSkip = new HashSet<>();
	
	
	private Map<String, Boolean> contractsDelay = new HashMap<>();
	private Map<String, Integer> contractsCount = new HashMap<>();
	private Map<String, String> publicSessionName = new HashMap<>();
	private int sessionCount = 0;
	
	@Override
	public void classLoaded(VM vm, ClassInfo ci) {

		if (ci.getName().equals(Participant.class.getName())) {
			if (participantTell == null)
				participantTell = ci.getMethod("tell", "(Lit/unica/co2/api/contract/ContractDefinition;Ljava/lang/Integer;)Lco2api/Public;", false); 
			
			if (participantWaitForSession == null)
				participantWaitForSession = ci.getMethod("waitForSession", "(Lco2api/Public;Ljava/lang/Integer;)Lit/unica/co2/api/Session2;", false); 
		}
		
		if (ci.getName().equals(CO2Process.class.getName())) {
			if (parallel == null)
				parallel = ci.getMethod("parallel", "(Ljava/lang/Runnable;)J", false); 
			
			if (processCall == null)
				processCall = ci.getMethod("processCall", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V", false);
			
			if (ifThenElse == null)
				ifThenElse = ci.getMethod("ifThenElse", "(Ljava/util/function/Supplier;Ljava/lang/Runnable;Ljava/lang/Runnable;)V", false);
			
			if (test == null)
				test = ci.getMethod("test", "()[Ljava/lang/String;", false);
		}
		
		if (ci.getName().equals(Session2.class.getName())) {
			if (sessionWaitForReceive==null)
				sessionWaitForReceive = ci.getMethod("waitForReceive", "(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;", false);
			
			if (sessionSend==null)
				sessionSend = ci.getMethod("send", "(Ljava/lang/String;)Ljava/lang/Boolean;", false);
			
			if (sessionSendString==null)
				sessionSendString = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Boolean;", false);
			
			if (sessionSendInt==null)
				sessionSendInt = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/Boolean;", false);
		}
		
		if (ci.getName().equals(Private.class.getName())) {
			
			if (privateTell==null)
				privateTell = ci.getMethod("tell", "(Ljava/lang/Integer;)Lco2api/Public;", false);
		}
		
		if (ci.getName().equals(Public.class.getName())) {
			
			if (waitForSession==null)
				waitForSession = ci.getMethod("waitForSession", "()Lco2api/Session;", false);
			
			if (waitForSessionT==null)
				waitForSessionT = ci.getMethod("waitForSession", "(Ljava/lang/Integer;)Lco2api/Session;", false);
		}

		if (ci.getName().equals(Session2.class.getName())) {
			
			if (waitForReceive==null)
				waitForReceive = ci.getMethod("waitForReceive", "(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;", false);
		}
		
		if (ci.getName().equals(Message.class.getName())) {
			
			if (messageGetStringValue==null)
				messageGetStringValue = ci.getMethod("getStringValue", "()Ljava/lang/String;", false);
		}
	}
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(ti);

		if(privateTell!=null && insn==privateTell.getFirstInsn()) {
			handleTell(ti, insn);
		}
		
		if(waitForSession!=null && insn==waitForSession.getFirstInsn()) {
			handleWaitForSession(ti, insn, false);
		}
		
		if(waitForSessionT!=null && insn==waitForSessionT.getFirstInsn()) {
			handleWaitForSession(ti, insn, true);
		}
		
		if(waitForReceive!=null && insn==waitForReceive.getFirstInsn()) {
			handleWaitForReceive(ti, insn);
		}
		
		if(messageGetStringValue!=null && insn==messageGetStringValue.getFirstInsn()) {
			handleMessageGetStringValue(ti, insn);
		}
		
//		if (test!=null && insn==test.getFirstInsn()) {
//			
//			if (!ti.isFirstStepInsn()) {
//				log.info("");
//				log.info("TEST (top-half)");
//				log.info("insn: "+insnToString(test.getFirstInsn()));
//				
//				IntChoiceFromList cg = new IntChoiceFromList(insn.getMethodInfo().getFullName(), 1, 42);
//				vm.setNextChoiceGenerator(cg);
//				ti.skipInstruction(insn);
//			}
//			else {
//				log.info("");
//				
//				// get the choice generator
//				IntChoiceFromList cg = vm.getSystemState().getCurrentChoiceGenerator(insn.getMethodInfo().getFullName(), IntChoiceFromList.class);
//				
//				// take a choice
//				
//				ElementInfo newResult = ti.getHeap().newArray("Ljava/lang/String;", 2, ti);
//				ElementInfo fstElement = ti.getHeap().newString("Hello", ti);
//				ElementInfo sndElement = ti.getHeap().newString("world!", ti);
//				newResult.setReferenceElement(0, fstElement.getObjectRef());
//				newResult.setReferenceElement(1, sndElement.getObjectRef());
//				
//				int ref = newResult.getObjectRef();
//				
//				
//				StackFrame frame = ti.getTopFrame();
//				frame.setReferenceResult(ref, null);
//				
//				Instruction nextInsn = new ARETURN();
//				nextInsn.setMethodInfo(test);
//				
//				log.info(insnToString(nextInsn));
//				
//				ti.skipInstruction(nextInsn);				
//			}
//		}
		
		if (
				methodsToSkip.contains(insn.getMethodInfo()) &&
				insn == insn.getMethodInfo().getFirstInsn()
				) {
			log.info("");
			log.info("SKIPPING METHOD: "+insn.getMethodInfo().getFullName());
			log.info("jumping to last insn: "+insnToString(insn.getMethodInfo().getLastInsn()));
			ti.skipInstruction(insn.getMethodInfo().getLastInsn());
		}
		else if (
				insn instanceof SwitchInstruction && 
				ci.isInstanceOf(CO2Process.class.getName())
				) {
			SwitchInstruction switchInsn = (SwitchInstruction) insn;
			/*
			 * switch statements use if-instructions that must be not considered for branch exploration
			 */
			log.finer("");
			log.finer("SWITCH : setting start="+switchInsn.getPosition()+" , end="+switchInsn.getTarget());
			tstate.setSwitchInsn(switchInsn);
		}
		else if (	
				insn instanceof IfInstruction &&
				insn.getMethodInfo() == ifThenElse
//				tstate.considerIfInstruction((IfInstruction) insn)
				) {
			
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

				BooleanChoiceGenerator cg = new BooleanChoiceGenerator(tstate.getBooleanChoiceGeneratorName(), false);

				vm.getSystemState().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				
			}
			else {

				log.finer("BOTTOM HALF");
				
				// bottom half - reexecution at the beginning of the next
				// transition
				BooleanChoiceGenerator cg = vm.getSystemState().getCurrentChoiceGenerator(tstate.getBooleanChoiceGeneratorName(), BooleanChoiceGenerator.class);

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


	private void handleWaitForReceive(ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("HANDLE -> WAIT FOR RECEIVE");
		
		//object Session2
		ElementInfo session2 = ti.getThisElementInfo();
		
		//parameters
		boolean hasTimeout = getArgumentInteger(ti, 0)>0;
		List<String> actions = getArgumentStringArray(ti, 1);
		
		assert actions.size()>=1 : "you must pass at least one action";
		
		log.info("timeout: "+hasTimeout);
		log.info("actions: "+actions);
		
		if (hasTimeout || actions.size()>1) {
			log.info("considering multiple choices");
			
			List<Integer> choiceSet = new ArrayList<>();

			actions.stream().forEach((x)-> {choiceSet.add(actions.indexOf(x));});
			if (hasTimeout) choiceSet.add(actions.size());

			assert choiceSet.size()>1;
			
			if (!ti.isFirstStepInsn()) {
				IntChoiceFromList cg = new IntChoiceFromList(insn.getMethodInfo().getFullName(), choiceSet.stream().mapToInt(i -> i).toArray());
				ti.getVM().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				return;
			}
			else {
				// get the choice generator
				IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(insn.getMethodInfo().getFullName(), IntChoiceFromList.class);
				
				// take a choice
				
				int choice = cg.getNextChoice();
				
				if (choice==actions.size()) {
					log.info("timeout expired, throwing a TimeExpiredException");
					ti.createAndThrowException(TimeExpiredException.class.getName(), "JPF listener");
					return;
				}
				else {
					String action = actions.get(choice);
					String value = "10";
					
					log.info("returning message: ["+action+":"+value+"]");
					
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
			
			log.info("returning a new Message");
			
			//build the return value
			ElementInfo messageEI = getMessage(ti, actions.get(0), "1");
			
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

	private void handleWaitForSession(ThreadInfo ti, Instruction insn, boolean hasTimeout) {
		log.info("");
		log.info("HANDLE -> WAIT FOR SESSION");
		
		//object Public
		ElementInfo pbl = ti.getThisElementInfo();
		
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
				IntChoiceFromList cg = new IntChoiceFromList(insn.getMethodInfo().getFullName(), choiceSet.stream().mapToInt(i -> i).toArray());
				ti.getVM().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				return;
			}
			else {
				// get the choice generator
				IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(insn.getMethodInfo().getFullName(), IntChoiceFromList.class);
				
				// take a choice
				switch (cg.getNextChoice()) {
				case 0:
					//do nothing, the return value is set at the end of the method
					break;

				case 1:
					log.info("delay expired, throwing a ContractExpiredException");
					ti.createAndThrowException(ContractExpiredException.class.getName(), "JPF listener");
					return;

				case 2:
					log.info("timeout expired, throwing a TimeExpiredException");
					ti.createAndThrowException(TimeExpiredException.class.getName(), "JPF listener");
					return;

				}
			}
		}
		else {
			log.info("single choice");
		}

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

	private void handleTell(ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("HANDLE -> TELL");
		
		//object Private
		ElementInfo pvt = ti.getThisElementInfo();
		
		log.info("object");
		log.info("\tpvt: "+pvt);

		//fields
		ElementInfo connection = ti.getElementInfo(pvt.getReferenceField("connection"));
		ElementInfo contract = ti.getElementInfo(pvt.getReferenceField("contract"));
		
		log.info("object's fields");
		log.info("\tcontract: "+contract);
		log.info("\tconnection: "+connection);

		//parameters
		int delay = getArgumentInteger(ti, 0);
		
		log.info("parameters");
		log.info("\tdelay: "+delay);
				
		String contractID = String.valueOf(contract.hashCode());

		Integer contractCount = contractsCount.get(contractID);
		contractCount = contractCount==null? 0 : contractCount+1;
		
		contractsCount.put(contractID, contractCount);
		contractID = "n:"+contractCount+contractID;
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
		
		log.info("storing <"+pblEI.toString()+","+"x"+sessionCount);
		if (!publicSessionName.containsKey(pblEI.toString()))
			publicSessionName.put(pblEI.toString(), "x"+sessionCount++);
	}


	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());

		if (exitedMethod==test) {
			log.info("");
			log.info("--TEST-- (method exited)");
		}
		
		if (exitedMethod == participantTell) {
			log.info("");
			tstate.printInfo();
			log.info("--TELL-- (method exited)");
			
			TellDS tell = new TellDS();
			SumDS sum = new SumDS(tell);
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			/*
			 * get the returned value
			 */
			StackFrame f = currentThread.getTopFrame();
			
			int ref = f.getReferenceResult();	// apparently this remove the reference to the stackframe
			f.setReferenceResult(ref, null);	// re-put the reference on the stackframe
			
			ElementInfo pbl = currentThread.getElementInfo(ref);

			String sessionName = publicSessionName.get(pbl.toString());
			
			ContractDefinition cDef = ObjectUtils.deserializeObjectFromStringQuietly(ei.getStringField("serializedContract"), ContractDefinition.class);
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
		else if (exitedMethod == participantWaitForSession) {

			log.info("");
			tstate.printInfo();
			log.info("--WAIT FOR SESSION-- (method exited)");
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDS sum = tstate.getSum();									//the sum of possible choice (set by entered-method)
			
			log.info("sumStack.peek(): "+sum.toString());
			
			if (ex!=null) {
				/* 
				 * if you were here, you invoke waitForSession() with a timeout
				 */

				if (ex.getExceptionClassname().equals(TimeExpiredException.class.getName())) {

					log.info("TIME_EXPIRED: "+ex.getExceptionClassname());
					
					assert sum.prefixes.size()==2 || sum.prefixes.size()==3;		// t . (...) + ask "" (True) . (...)
					
					//get the tau prefix
					TauDS tau = null;
					
					if (sum.prefixes.get(0) instanceof TauDS)
						tau=(TauDS) sum.prefixes.get(0);
					else
						tau=(TauDS) sum.prefixes.get(1);
					
					tstate.popSum(tau);
					tstate.setCurrentPrefix(tau);
				}
				else  if (ex.getExceptionClassname().equals(ContractExpiredException.class.getName())) {
					
					log.info("CONTRACT_EXPIRED: "+ex.getExceptionClassname());
					
					// the exception can be thrown by any waitForSession() (timeout or not)
					
					/*
					 * get the returned value
					 */
					StackFrame f = currentThread.getTopFrame();
					
					int ref = f.getReferenceResult();	// apparently this remove the reference to the stackframe
					f.setReferenceResult(ref, null);	// re-put the reference on the stackframe
					
					ElementInfo session2 = currentThread.getElementInfo(ref);
					
					ElementInfo pbl = currentThread.getElementInfo(session2.getReferenceField("contract"));
					assert pbl!=null;
					String sessionName = publicSessionName.get(pbl.toString());
					
					RetractDS retract = new RetractDS();
					retract.session = sessionName;
					
					sum.prefixes.add(retract);
					tstate.setCurrentPrefix( retract);
				}
				else {
					throw new IllegalStateException("unexpected exception: "+ex.getExceptionClassname());
				}
				
			}
			else {
				
				log.info("normal behaviour");
				/*
				 * get the returned value
				 */
				StackFrame f = currentThread.getTopFrame();
				
				int ref = f.getReferenceResult();	// apparently this remove the reference to the stackframe
				f.setReferenceResult(ref, null);	// re-put the reference on the stackframe
				
				ElementInfo session2 = currentThread.getElementInfo(ref);
				
				ElementInfo pbl = currentThread.getElementInfo(session2.getReferenceField("contract"));
				assert pbl!=null;
				String sessionName = publicSessionName.get(pbl.toString());
				
				log.info("session: "+sessionName);
				
				assert sum.prefixes.size()==1 || sum.prefixes.size()==2 || sum.prefixes.size()==3;		// the t prefix can not be present when waitForSession is invoked without timeout
				
				AskDS ask = null;

				if (sum.prefixes.size()==1) {
					ask = (AskDS) sum.prefixes.get(0);
				}
				else if (sum.prefixes.get(0) instanceof AskDS) {
					//sum.prefixes.size()==2
					ask = (AskDS) sum.prefixes.get(0);
				}
				else {
					//sum.prefixes.size()==2
					ask = (AskDS) sum.prefixes.get(1);
				}
				
				if (ask==null)
					throw new IllegalStateException("ask cannot be null");
				
				
				ask.session = sessionName;
				
				tstate.popSum(ask);
				tstate.setCurrentPrefix(ask);		//set the current prefix
			}
			
			tstate.printInfo();
		}
		else if (exitedMethod == sessionWaitForReceive) {

			log.info("");
			tstate.printInfo();
			log.info("--SUM OF RECEIVE-- (method exited)");
			
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDS sum = tstate.getSum();									//the sum of possible choice (set by entered-method)
			log.info("sumStack.peek(): "+sum.toString());
			
			if (ex!=null) {
				/* 
				 * if you were here, you have call waitForReceive(Integer,String...) with a timeout
				 * a prefix tau must be present at this time
				 */
				log.info("TIME_EXPIRED: "+ex.getExceptionClassname());
				
				assert sum.prefixes.size()>0;
				assert sum.prefixes.get(sum.prefixes.size()-1) instanceof TauDS;
				
				TauDS p = (TauDS) sum.prefixes.get(sum.prefixes.size()-1);
				
				tstate.popSum(p);
				tstate.setCurrentPrefix(p);
			}
			else {
				
				ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)

				/*
				 * get the returned value
				 */
				StackFrame f = currentThread.getTopFrame();
				
				int ref = f.getReferenceResult();	// apparently this remove the reference to the stackframe
				f.setReferenceResult(ref, null);	// re-put the reference on the stackframe
				
				ElementInfo messageReceived = currentThread.getElementInfo(ref);
				
				String label = messageReceived.getStringField("label");
				
				ElementInfo pbl = currentThread.getElementInfo(session2.getReferenceField("contract"));
				assert pbl!=null;
				String sessionName = publicSessionName.get(pbl.toString());
				
				log.info("label: "+label);
				
				DoReceiveDS received = null;
				
				for (int i=0; i<sum.prefixes.size(); i++) {
					
					PrefixDS p = sum.prefixes.get(i);
					
					if (
							(p instanceof DoReceiveDS) && 
							((DoReceiveDS) p).action.equals(label)
							) {
						received = ((DoReceiveDS) sum.prefixes.get(i));
						break;
					}
					
				}
				
				if (received==null)
					throw new IllegalStateException("received cannot be null");
				
				received.action = label;
				received.session = sessionName;
				
				tstate.popSum(received);
				tstate.setCurrentPrefix(received);		//set the current prefix
			}
			
			tstate.printInfo();
		}
		else if (
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
		
		if (enteredMethod == participantWaitForSession) {

			log.info("");
			tstate.printInfo();
			log.info("--WAIT FOR SESSION-- (entered method)");
			
			Integer timeout = getArgumentInteger(currentThread, 1);
			
			log.info("timeout: "+timeout);

			SumDS sum = new SumDS();
			sum.prefixes.add(new AskDS());
			
			if (timeout==-1) {
				/* nothing to do */
			}
			else {
				/* add tau prefix, used if TimeExpiredException */
				sum.prefixes.add(new TauDS());
			}
			
			tstate.setCurrentProcess(sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			tstate.pushSum(sum);
			
			tstate.printInfo();
		}
		else if (enteredMethod == sessionWaitForReceive) {

			log.info("");
			tstate.printInfo();
			log.info("--SUM OF RECEIVE-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			ElementInfo pbl = currentThread.getElementInfo(session2.getReferenceField("contract"));
			assert pbl!=null;
			String sessionName = publicSessionName.get(pbl.toString());
			
			Integer timeout = getArgumentInteger(currentThread, 0);
			List<String> actions = getArgumentStringArray(currentThread, 1);
			
			
			SumDS sum = new SumDS();
			
			for (String l : actions) {
				
				DoReceiveDS p = new DoReceiveDS(); 
				p.session = sessionName;
				p.action = l;
				
				sum.prefixes.add(p);
			}
			
			
			log.info("timeout: "+timeout);
			log.info("actions: "+actions);
			
			if (timeout==-1) {
				/* nothing to do */
			}
			else {
				/* add tau prefix, used if TimeExpiredException */
				sum.prefixes.add(new TauDS());
			}
			
			tstate.setCurrentProcess(sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			tstate.pushSum(sum);
			
			tstate.printInfo();
		}
		else if (enteredMethod==sessionSend || enteredMethod==sessionSendInt || enteredMethod==sessionSendString) {

			log.info("");
			tstate.printInfo();
			log.info("--SEND-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			ElementInfo pbl = currentThread.getElementInfo(session2.getReferenceField("contract"));
			assert pbl!=null;
			String sessionName = publicSessionName.get(pbl.toString());
			String action = getArgumentString(currentThread, 0);
			
			DoSendDS send = new DoSendDS();
			send.session = sessionName;
			send.action = action;
			
			if (enteredMethod==sessionSend) send.sort = Sort.UNIT;
			if (enteredMethod==sessionSendInt) send.sort = Sort.INT;
			if (enteredMethod==sessionSendString) send.sort = Sort.STRING;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(send);
			
			tstate.setCurrentProcess(sum);		//set the current process
			tstate.setCurrentPrefix(send);		//set the current prefix

			tstate.printInfo();
		}
		else if (enteredMethod==parallel) {
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
		else if (enteredMethod==processCall) {
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
	

	@SuppressWarnings("unused")
	private String insnToString(Instruction insn) {
		return insn.getPosition() + " - " + insn.getMnemonic() + " ("+insn.getClass()+")";
	}
	
	/**
	 * Get the actual parameters of the given thread
	 * @param currentThread
	 * @return
	 */
	private Object[] getArguments(ThreadInfo currentThread) {
		return currentThread.getTopFrame().getArgumentValues(currentThread);
	}
	
	private String getArgumentString(ThreadInfo currentThread, int position) {
		ElementInfo eiArgument = (ElementInfo) getArguments(currentThread)[position];
		return eiArgument.asString();
	}
	
	private Integer getArgumentInteger(ThreadInfo currentThread, int position) {
		ElementInfo eiArgument = (ElementInfo) getArguments(currentThread)[position];
		return (Integer) eiArgument.asBoxObject();
	}
	
	private List<ElementInfo> getArgumentArray(ThreadInfo currentThread, int position) {
		List<ElementInfo> elms = new ArrayList<>();
		
		ElementInfo eiArgument = (ElementInfo) getArguments(currentThread)[position];
		ArrayFields af = eiArgument.getArrayFields();
		
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
