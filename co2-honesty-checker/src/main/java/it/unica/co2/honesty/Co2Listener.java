package it.unica.co2.honesty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import co2api.CO2ServerConnection;
import co2api.ContractExpiredException;
import co2api.ContractModel;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.SessionI;
import co2api.TST;
import co2api.TimeExpiredException;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVMInstructionFactory;
import gov.nasa.jpf.jvm.JVMStackFrame;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.ArrayFields;
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
import it.unica.co2.api.contract.Action;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.contract.Sort.IntegerSort;
import it.unica.co2.api.contract.Sort.StringSort;
import it.unica.co2.api.contract.Sum;
import it.unica.co2.api.contract.utils.ContractExplorer;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.MultipleSessionReceiver;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.dto.CO2DataStructures.AskDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoReceiveDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoSendDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ParallelProcessesDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.RetractDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;
import it.unica.co2.honesty.handlers.HandlerFactory;
import it.unica.co2.honesty.handlers.IfThenElseHandler;
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
	 * all advertised contracts
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
	private MethodInfo Session_waitForReceive;
	private MethodInfo Session_sendIfAllowed;
	private MethodInfo Session_sendIfAllowedString;
	private MethodInfo Session_sendIfAllowedInt;
	private MethodInfo Message_getStringValue;
	private MethodInfo LoggerFactory_getLogger;
	private MethodInfo MultipleSessionReceiver_waitForReceive;
	
	// collect the 'run' methods in order to avoid re-build of an already visited CO2 process
	private Map<MethodInfo, AnnotationInfo> methodsToSkip = new HashMap<>();
	
	private Map<String, Boolean> contractsDelay = new HashMap<>();
	private Map<String, String> publicSessionName = new HashMap<>();

	private Map<String, Map<String, Sort<?>>> contractActionsSort = new HashMap<>();
	
	
	
	
	@Override
	public void classLoaded(VM vm, ClassInfo ci) {

			
		for (MethodInfo m : ci.getDeclaredMethodInfos() ) {
			
			AnnotationInfo ai = m.getAnnotation(SkipMethod.class.getName());
			
			if (ai!=null) {
				log.info("[SKIP] adding method "+m.getFullName());
				methodsToSkip.put(m, ai);
			}
		}
		
		if (ci.getName().equals(Participant.class.getName())) {
			if (Participant_tell == null)
				Participant_tell = ci.getMethod("_tell", "(Lit/unica/co2/api/contract/ContractDefinition;Ljava/lang/String;Lco2api/Private;Ljava/lang/Integer;)Lco2api/Public;", false); 
			
			if (Participant_setConnection==null)
				Participant_setConnection = ci.getMethod("setConnection", "()V", false);
		}
		
		if (ci.getName().equals(CO2Process.class.getName())) {
			if (CO2Process_parallel == null)
				CO2Process_parallel = ci.getMethod("parallel", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", false); 
			
			if (CO2Process_processCall == null)
				CO2Process_processCall = ci.getMethod("processCall", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V", false);
		}
		
		if (ci.getName().equals(Public.class.getName())) {
			
			if (Public_waitForSession==null)
				Public_waitForSession = ci.getMethod("waitForSession", "()Lco2api/Session;", false);
			
			if (Public_waitForSessionT==null)
				Public_waitForSessionT = ci.getMethod("waitForSession", "(Ljava/lang/Integer;)Lco2api/Session;", false);
		}

		if (ci.getName().equals(Session.class.getName())) {
			
			if (Session_waitForReceive==null)
				Session_waitForReceive = ci.getMethod("waitForReceive", "(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;", false);
			
			if (Session_sendIfAllowed==null)
				Session_sendIfAllowed = ci.getMethod("sendIfAllowed", "(Ljava/lang/String;)Z", false);
			
			if (Session_sendIfAllowedString==null)
				Session_sendIfAllowedString = ci.getMethod("sendIfAllowed", "(Ljava/lang/String;Ljava/lang/String;)Z", false);
			
			if (Session_sendIfAllowedInt==null)
				Session_sendIfAllowedInt = ci.getMethod("sendIfAllowed", "(Ljava/lang/String;Ljava/lang/Integer;)Z", false);
			
			methodsToSkip.put(ci.getMethod("amIOnDuty", "()Z", false), null);
			methodsToSkip.put(ci.getMethod("amICulpable", "()Z", false), null);
		}
		
		if (ci.getName().equals(Message.class.getName())) {
			
			if (Message_getStringValue==null)
				Message_getStringValue = ci.getMethod("getStringValue", "()Ljava/lang/String;", false);
		}
		
		if (ci.getName().equals(TST.class.getName())) {
			methodsToSkip.put(ci.getMethod("setFromString", "(Ljava/lang/String;)V", false), null);
			methodsToSkip.put(ci.getMethod("setContext", "(Ljava/lang/String;)V", false), null);
		}
		
		if (ci.getName().equals(LoggerFactory.class.getName())) {
			
			if (LoggerFactory_getLogger==null)
				LoggerFactory_getLogger = ci.getMethod("getLogger", "(Ljava/lang/String;)Lorg/slf4j/Logger;", false);
		}
		
		if (ci.getName().equals(MultipleSessionReceiver.class.getName())) {
			if (MultipleSessionReceiver_waitForReceive==null)
				MultipleSessionReceiver_waitForReceive = ci.getMethod("_waitForReceive", "(I)V", false);
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
			handle_Participant_tell(tstate, ti, insn);
		}
		else if(Public_waitForSession!=null && insn==Public_waitForSession.getFirstInsn()) {
			handle_Public_waitForSession(tstate, ti, insn, false);
		}
		else if(Public_waitForSessionT!=null && insn==Public_waitForSessionT.getFirstInsn()) {
			handle_Public_waitForSession(tstate, ti, insn, true);
		}
		else if(Session_waitForReceive!=null && insn==Session_waitForReceive.getFirstInsn()) {
			handle_Session_waitForReceive(tstate, ti, insn);
		}
		else if(Message_getStringValue!=null && insn==Message_getStringValue.getFirstInsn()) {
			handle_Message_getStringValue(ti, insn);
		}
		else if (insn instanceof SwitchInstruction && tstate.considerSwitchInstruction((SwitchInstruction) insn)) {
			tstate.setSwitchInsn((SwitchInstruction) insn);
		}
		else if (insn instanceof IfInstruction && tstate.considerIfInstruction((IfInstruction) insn)) {
			HandlerFactory.getHandler(IfThenElseHandler.class).handle(tstate, ti, insn);
		}
		else if (
				(Session_sendIfAllowed!=null && insn==Session_sendIfAllowed.getFirstInsn()) ||
				(Session_sendIfAllowedInt!=null && insn==Session_sendIfAllowedInt.getFirstInsn()) ||
				(Session_sendIfAllowedString!=null && insn==Session_sendIfAllowedString.getFirstInsn())
				) {
			handle_Session_sendIfAllowed(tstate, ti, insn);
		}
		else if(Participant_setConnection!=null && insn==Participant_setConnection.getFirstInsn()) {
			handle_Participant_setConnection(ti, insn);
		}
		else if (LoggerFactory_getLogger!=null && insn==LoggerFactory_getLogger.getFirstInsn()) {
			handle_LoggerFactory_getLogger(ti, insn);
		}
		else if (MultipleSessionReceiver_waitForReceive!=null && insn==MultipleSessionReceiver_waitForReceive.getFirstInsn()) {
			handle_MultipleSessionReceiver_waitForReceive(ti, insn);
		}
		else if (methodsToSkip.containsKey(insn.getMethodInfo()) && insn == insn.getMethodInfo().getFirstInsn()) {
			handleSkipMethod(tstate, ti, insn);
		}
	}
	
	
	private void handle_LoggerFactory_getLogger(ThreadInfo ti, Instruction insn) {

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
	
	private void handle_MultipleSessionReceiver_waitForReceive(ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("-- WAIT FOR RECEIVE (multi session) --");
		
		//parameters
		boolean timeout = getArgumentInt(ti, 0)>0;
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
			
			log.info("session: "+publicSessionName.get(session.getPublicContract().getUniqueID()));
			log.info("actions: "+actions);
		}
		
		ElementInfo consumersEI = mReceiver.getObjectField("consumersArray");
		int[] consumersRefs = consumersEI.asReferenceArray();

		log.info("number of consumers: "+consumersRefs.length);
		
		ElementInfo consumerEI = ti.getElementInfo(consumersRefs[0]);
		
		// create a message
		ElementInfo message = getMessage(ti, "foo", "foo value");
		
		MethodInfo consumerMI = consumerEI.getClassInfo().getMethod("accept", "(Lco2api/Message;)V", false);
		
		log.info(consumerEI.toString());
		log.info(consumerMI.toString());
		
		// create a new frame with the message
		ti.getModifiableTopFrame().pushRef(consumerEI.getObjectRef());	// object ref
		ti.getModifiableTopFrame().pushRef(message.getObjectRef());		// arguments
		
		
		Instruction nextInsn = JVMInstructionFactory.getFactory().invokespecial(consumerEI.getClassInfo().getName(), consumerMI.getName(), consumerMI.getSignature());
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}

	private void handle_Participant_setConnection(ThreadInfo ti, Instruction insn) {
		
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


	private void handle_Session_sendIfAllowed(ThreadState tstate, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("-- SEND --");
		
		/*
		 * collect the co2 process
		 */
		ElementInfo session2 = ti.getThisElementInfo();
		
		String sessionName = getSessionNameBySession(ti, session2);
		String action = getArgumentString(ti, 0);
		
		DoSendDS send = new DoSendDS();
		send.session = sessionName;
		send.action = action;
		
		if (insn.getMethodInfo()==Session_sendIfAllowed) send.sort = Sort.unit();
		if (insn.getMethodInfo()==Session_sendIfAllowedInt) send.sort = Sort.integer();
		if (insn.getMethodInfo()==Session_sendIfAllowedString) send.sort = Sort.string();
		
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
	
	
	
	private String getSessionNameBySession(ThreadInfo ti, ElementInfo session) {
		ElementInfo pbl = ti.getElementInfo(session.getReferenceField("contract"));
		return getSessionNameByPublic(pbl);
	}
	
	private String getSessionNameByPublic(ElementInfo pbl) {
		String sessionName = publicSessionName.get(getUniqueIDByPublic(pbl));
		assert sessionName!=null;
		return sessionName;
	}
	
	private String getUniqueIDBySession(ThreadInfo ti, ElementInfo session) {
		ElementInfo pbl = ti.getElementInfo(session.getReferenceField("contract"));
		return getUniqueIDByPublic(pbl);
	}
	
	private String getUniqueIDByPublic(ElementInfo pbl) {
		return pbl.getStringField("uniqueID");
	}
	
	
	private void handleSkipMethod(ThreadState tstate, ThreadInfo ti, Instruction insn) {
		log.info("");
		log.info("SKIPPING METHOD: "+insn.getMethodInfo().getFullName());
		

		AnnotationInfo ai = methodsToSkip.get(insn.getMethodInfo());
		String[] exceptions = insn.getMethodInfo().getThrownExceptionClassNames();
		
		if (ai!=null && exceptions!=null) {	// the method contains the @SkipMethod annotation
			
			log.info("declared exceptions: "+Arrays.toString(exceptions));
			
			//remove empty strings (default case)
			exceptions = Arrays.stream(exceptions).filter(x -> !x.isEmpty()).toArray(String[]::new);
			
			int[] choiceSet = new int[exceptions.length+1];
			for (int i=0; i<exceptions.length; i++) {
				choiceSet[i] = i;
				try {
					Class.forName(exceptions[i]);
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException("cannot find the class '"+exceptions[i]+"'");
				}
			}
			choiceSet[exceptions.length] = exceptions.length;
			
			if (exceptions.length>0) {
				// add a new boolean choice generator
				
				if (!ti.isFirstStepInsn()) {
					
					log.info("[skip] TOP HALF");
					
					SumDS sum = new SumDS();
					
					tstate.setCurrentProcess(sum);		//set the current process
					tstate.printInfo();
					tstate.pushSum(sum, Stream.concat(
							Stream.of("$norm"), 
							Arrays.stream(Arrays.copyOfRange(choiceSet, 0, exceptions.length)).mapToObj(Integer::toString).map(x-> "$ex".concat(x))).toArray(String[]::new));
					
					IntChoiceFromList cg = new IntChoiceFromList(tstate.getSkipMethodRuntimeExceptionGeneratorName(), choiceSet);
					
					boolean cgSetOk = ti.getVM().getSystemState().setNextChoiceGenerator(cg);
					
					assert cgSetOk : "error setting the choice generator";
					
					ti.skipInstruction(insn);
					log.info("re-executing: "+insnToString(insn));
					
					return;				
				}
				else {
					log.info("[skip] BOTTOM HALF");
					
					// bottom half - reexecution at the beginning of the next
					// transition
					IntChoiceFromList cg = ti.getVM().getSystemState().getCurrentChoiceGenerator(tstate.getSkipMethodRuntimeExceptionGeneratorName(), IntChoiceFromList.class);
					
					assert cg != null : "no 'skipMethod_booleanGenerator' BooleanChoiceGenerator found";
					
					int myChoice = cg.getNextChoice();
					
					if (myChoice!=exceptions.length){
						/*
						 * throw corresponding Exception
						 */
						String exception = exceptions[myChoice];
						log.info("[skip] throwing a "+exception);
						
						SumDS sum = tstate.getSum();
						TauDS tau = new TauDS();
						sum.prefixes.add(tau);
						
						tstate.popSum("$ex"+myChoice);
						tstate.setCurrentPrefix(tau);
						tstate.printInfo();
						
						ti.createAndThrowException(exception, "This exception is thrown by the honesty checker. Please catch it!");
						
						return;
					}
					else {
						/*
						 * continue normally
						 */
						log.info("[skip] continue normally");
						
						SumDS sum = tstate.getSum();
						TauDS tau = new TauDS();
						sum.prefixes.add(tau);
						
						tstate.popSum("$norm");
						tstate.setCurrentPrefix(tau);
						tstate.printInfo();
						
					}
				}
			}
		}
		
		
		//TODO: considera un refactoring del codice seguente
		Instruction nextInsn = null;
		
		switch (insn.getMethodInfo().getReturnTypeCode()) {
		
		case Types.T_BOOLEAN:
			if (ai!=null) {
				//set the return value
				boolean returnValue = ai.valueAsString().isEmpty()? true: Boolean.parseBoolean(ai.valueAsString());
				log.info("[skip] setting BOOLEAN return value: "+returnValue);
				ti.getModifiableTopFrame().push(returnValue? 1:0);
			}
			nextInsn = new IRETURN();
			break;
			
		case Types.T_CHAR: 
			if (ai!=null) {
				//set the return value
				char returnValue = ai.valueAsString().isEmpty()? 'a': ai.valueAsString().charAt(0);
				log.info("[skip] setting INT return value: "+returnValue);
				ti.getModifiableTopFrame().push(returnValue);
			}
			nextInsn = new IRETURN();
			break;
		
		case Types.T_BYTE:
		case Types.T_SHORT: 
		case Types.T_INT:
			if (ai!=null) {
				//set the return value
				int returnValue = ai.valueAsString().isEmpty()? 0 : Integer.parseInt(ai.valueAsString());
				log.info("[skip] setting INT return value: "+returnValue);
				ti.getModifiableTopFrame().push(returnValue);
			}
			nextInsn = new IRETURN();
			break;
			
		case Types.T_LONG:
			if (ai!=null) {
				//set the return value
				long returnValue = ai.valueAsString().isEmpty()? 0: Long.parseLong(ai.valueAsString());
				log.info("[skip] setting LONG return value: "+returnValue);
				ti.getModifiableTopFrame().pushLong(returnValue);
			}
			nextInsn = new LRETURN();
			break;
			
		case Types.T_FLOAT:
			if (ai!=null) {
				//set the return value
				float returnValue = ai.valueAsString().isEmpty()? 0: Float.parseFloat(ai.valueAsString());
				log.info("[skip] setting FLOAT return value: "+returnValue);
				ti.getModifiableTopFrame().pushFloat(returnValue);
			}
			nextInsn = new FRETURN();
			break;
			
		case Types.T_DOUBLE:
			if (ai!=null) {
				//set the return value
				double returnValue = ai.valueAsString().isEmpty()? 0: Double.parseDouble(ai.valueAsString());
				log.info("[skip] setting DOUBLE return value: "+returnValue);
				ti.getModifiableTopFrame().pushDouble(returnValue);
			}
			nextInsn = new DRETURN();
			break;
			
		case Types.T_ARRAY:
		case Types.T_REFERENCE:
			if (ai!=null) {
				//set the return value
				String returnValue = ai.valueAsString();
				log.info("[skip] setting REF return value: "+returnValue);
				ti.getModifiableTopFrame().pushRef(ti.getHeap().newString(returnValue, ti).getObjectRef());
			}
			nextInsn = new ARETURN();
			break;
			
			
		case Types.T_VOID:
		default: nextInsn = new RETURN();
		}
		
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}


	private void handle_Message_getStringValue(ThreadInfo ti, Instruction insn) {
		//bypass the type check of the message
		
		ElementInfo message = ti.getThisElementInfo();
		
		//set the return value
		StackFrame frame = ti.getTopFrame();
		frame.setReferenceResult(message.getReferenceField("stringVal"), null);
		
		Instruction nextInsn = new ARETURN();
		nextInsn.setMethodInfo(insn.getMethodInfo());
		
		ti.skipInstruction(nextInsn);
	}


	private void handle_Session_waitForReceive(ThreadState ts, ThreadInfo ti, Instruction insn) {
		
		log.info("");
		log.info("-- WAIT FOR RECEIVE --");
		
		ElementInfo session2 = ti.getThisElementInfo();
		
		String sessionName = getSessionNameBySession(ti, session2);
		String contractUniqueID = getUniqueIDBySession(ti, session2);
		
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
				
				if (timeout) {
					String[] arr = new String[actions.size()+1];
					arr[0] = "t";
					actions.stream().forEach((x)->{arr[actions.indexOf(x)+1]=x;});
					ts.pushSum(sum, arr);		//set the current process
				}
				else
					ts.pushSum(sum, actions.toArray(new String[]{}));
					
				ts.setCurrentProcess(sum);
				ts.printInfo();
				
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
				
				assert cg!=null : "choice generator not found: "+ts.getWaitForReceiveChoiceGeneratorName();
				
				// take a choice
				int choice = cg.getNextChoice();
				
				if (choice==actions.size()) {
					
					log.info("timeout expired");
					
					TauDS tau = new TauDS();
					sum.prefixes.add(tau);
					
					log.info("setting current prefix: "+tau);
					ts.popSum(tau);
					ts.setCurrentPrefix(tau);
					ts.printInfo();
					
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
					Sort<?> sort = contractActionsSort.get(contractUniqueID).get(action);
					
					String value = getValidValue(sort);

					log.info("returning message: ["+action+":"+value+"]");
					
					DoReceiveDS p = new DoReceiveDS(); 
					p.session = sessionName;
					p.action = action;
					
					sum.prefixes.add(p);
					
					log.info("setting current prefix: "+p);
					ts.popSum(p);
					ts.setCurrentPrefix(p);
					ts.printInfo();
					
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
			Sort<?> sort = contractActionsSort.get(contractUniqueID).get(action);

			String value = getValidValue(sort);

			DoReceiveDS p = new DoReceiveDS(); 
			p.session = sessionName;
			p.action = action;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(p);
			
			log.info("returning message: ["+action+":"+value+"]");
			
			log.info("setting current process: "+sum);
			log.info("setting current prefix: "+p);
			ts.setCurrentProcess(sum);		//set the current process
			ts.setCurrentPrefix(p);
			ts.printInfo();
			
			//build the return value
			ElementInfo messageEI = getMessage(ti, action, value);
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(messageEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
		
		
	}

	
	private String getValidValue(Sort<?> sort) {
		String value = "0";		//for backward compatibility

		//get the validValue from Sort
		if (sort instanceof StringSort) {
			value = ((StringSort) sort).getValidValue();
		}
		else if (sort instanceof IntegerSort) {
			value = ((IntegerSort) sort).getValidValue().toString();
		}
		return value;
	}
	
	
	private ElementInfo getMessage(ThreadInfo ti, String label, String value) {
		
		assert label!=null;
		assert value!=null;
		
		ClassInfo messageCI = ClassInfo.getInitializedClassInfo(Message.class.getName(), ti);
		ElementInfo messageEI = ti.getHeap().newObject(messageCI, ti);
		
		messageEI.setReferenceField("label", ti.getHeap().newString(label, ti).getObjectRef());
		messageEI.setReferenceField("stringVal", ti.getHeap().newString(value, ti).getObjectRef());
		
		return messageEI;
	}

	
	private void handle_Public_waitForSession(ThreadState tstate, ThreadInfo ti, Instruction insn, boolean hasTimeout) {
		log.info("");
		log.info("-- WAIT FOR SESSION --");
		
		//object Public
		ElementInfo pbl = ti.getThisElementInfo();
		
		String sessionName = getSessionNameByPublic(pbl);
		
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
					
					AskDS ask = new AskDS();
					ask.session = sessionName;
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

			AskDS ask = new AskDS();
			ask.session = sessionName;
			
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
			
			//set the return value
			StackFrame frame = ti.getTopFrame();
			frame.setReferenceResult(sessionEI.getObjectRef(), null);
			
			Instruction nextInsn = new ARETURN();
			nextInsn.setMethodInfo(insn.getMethodInfo());
			
			ti.skipInstruction(nextInsn);
		}
		
	}

	
	private void handle_Participant_tell(ThreadState tstate, ThreadInfo ti, Instruction insn) {
		
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
		
		//get a unique ID for the contract (in order to handle delays appropriately when waitForSession is invoked)
		String contractID = NameProvider.getFreeName("c_");
		String sessionID = NameProvider.getFreeName("s_");
		
		log.info("binding contractID with sessionID: <"+contractID+","+sessionID+">");
		publicSessionName.put(contractID, sessionID);
		
		log.info("saving contract delay: <"+contractID+","+sessionID+">");
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
		
		
		
		
		
		TellDS tell = new TellDS();
		SumDS sum = new SumDS(tell);
		
		ContractDefinition cDef = ObjectUtils.deserializeObjectFromStringQuietly(cserial, ContractDefinition.class);
		contracts.put(cDef.getName(), cDef);
		
		log.info("addind contract: "+cDef.getName());
		
		for (ContractDefinition ref : ContractExplorer.getAllReferences(cDef)) {
			log.info("adding contract: "+ref.getName());
			contracts.put(ref.getName(), ref);
		}
		
		Map<String, Sort<?>> actionSortMap = new HashMap<>();
		
		log.info("action-sort mapping");
		ContractExplorer.findAll(
				cDef.getContract(),
				Sum.class,
				(x) -> {
					for (Object obj : x.getActions()) {
						Action a = (Action) obj;
						log.info(a.getName() + " - " + a.getSort().getValidValue());
						actionSortMap.put(a.getName(), a.getSort());
					}
				});
		
		for (ContractDefinition ref : ContractExplorer.getAllReferences(cDef)) {
			ContractExplorer.findAll(
					ref.getContract(),
					Sum.class,
					(x) -> {
						for (Object obj : x.getActions()) {
							Action a = (Action) obj;
							log.info(a.getName() + " - " + a.getSort().getValidValue());
							actionSortMap.put(a.getName(), a.getSort());
						}
					});
		}

		contractActionsSort.put(contractID, actionSortMap);
		
		sessions.add(sessionID);
		
		tell.contractName = cDef.getName();
		tell.session = sessionID;
		
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
		ThreadState tstate = threadStates.get(currentThread);
		
		if (enteredMethod==CO2Process_parallel) {
			log.info("");
			log.info("--PARALLEL-- (method entered) -> ID:"+tstate.getId());
			
			ParallelProcessesDS parallel = new ParallelProcessesDS();
			
			SumDS sumA = new SumDS();
			PrefixPlaceholderDS placeholderA = new PrefixPlaceholderDS();
			sumA.prefixes.add(placeholderA);
			
			SumDS sumB = new SumDS();
			PrefixPlaceholderDS placeholderB = new PrefixPlaceholderDS();
			sumB.prefixes.add(placeholderB);
			
			parallel.processA = sumA;
			parallel.processB = sumB;
			
			tstate.setCurrentProcess(parallel);
			tstate.setCurrentPrefix(placeholderB);
			
			threadCurrentProcess = sumA;
			threadCurrentPrefix = placeholderA;
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
					if (ei.getClassInfo().isInstanceOf(SessionI.class.getName())) {
						ElementInfo pbl = currentThread.getElementInfo(ei.getReferenceField("contract"));
						assert pbl!=null;
						String sessionName = getSessionNameByPublic(pbl);
						
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
				
				if (ei.getClassInfo().isInstanceOf(SessionI.class.getName())) {
					ElementInfo pbl = currentThread.getElementInfo(ei.getReferenceField("contract"));
					assert pbl!=null;
					String sessionName = getSessionNameByPublic(pbl);
					
					pCall.params.add("\""+sessionName+"\"");
					log.info("param: Session ("+sessionName+")");
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
				
				log.info("[SKIP] [T-ID "+tstate.getId()+"] adding method "+enteredMethod.getFullName());
				methodsToSkip.put(enteredMethod, null);
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
	
	private int getArgumentInt(ThreadInfo currentThread, int position) {
		return (int) getArguments(currentThread)[position];
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
