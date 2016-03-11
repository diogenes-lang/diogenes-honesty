package it.unica.co2.honesty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.SessionI;
import co2api.TST;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
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
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.handlers.HandlerFactory;

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
	private Set<MethodInfo> methodsToSkip = new HashSet<>();
	
	private Map<String, Boolean> contractsDelay = new HashMap<>();
	private Map<String, String> contractSessionMap = new HashMap<>();

	private Map<String, Map<String, Sort<?>>> contractActionsSort = new HashMap<>();
	
	
	
	
	@Override
	public void classLoaded(VM vm, ClassInfo ci) {

		for (MethodInfo m : ci.getDeclaredMethodInfos() ) {
			
			AnnotationInfo ai = m.getAnnotation(SkipMethod.class.getName());
			
			if (ai!=null) {
				log.info("[SKIP] adding method "+m.getFullName());
				methodsToSkip.add(m);
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
			
			methodsToSkip.add(ci.getMethod("amIOnDuty", "()Z", false));
			methodsToSkip.add(ci.getMethod("amICulpable", "()Z", false));
		}
		
		if (ci.getName().equals(Message.class.getName())) {
			
			if (Message_getStringValue==null)
				Message_getStringValue = ci.getMethod("getStringValue", "()Ljava/lang/String;", false);
		}
		
		if (ci.getName().equals(TST.class.getName())) {
			methodsToSkip.add(ci.getMethod("setFromString", "(Ljava/lang/String;)V", false));
			methodsToSkip.add(ci.getMethod("setContext", "(Ljava/lang/String;)V", false));
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
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ThreadState tstate = threadStates.get(ti);

//		ClassInfo ci = ti.getExecutingClassInfo();
//		if (ci.getName().equals(processUnderTestClass.getName())) {
//			log.info("*** "+insnToString(insn));
//		}
		
		if(Participant_tell!=null && insn==Participant_tell.getFirstInsn()) {
			HandlerFactory.tellHandler().handle(this, tstate, ti, insn);
		}
		else if(Public_waitForSession!=null && insn==Public_waitForSession.getFirstInsn()) {
			HandlerFactory.waitForSessionHandler().handle(this, tstate, ti, insn);
		}
		else if(Public_waitForSessionT!=null && insn==Public_waitForSessionT.getFirstInsn()) {
			HandlerFactory.waitForSessionHandler(true).handle(this, tstate, ti, insn);
		}
		else if(Session_waitForReceive!=null && insn==Session_waitForReceive.getFirstInsn()) {
			HandlerFactory.waitForReceiveHandler().handle(this, tstate, ti, insn);
		}
		else if(Message_getStringValue!=null && insn==Message_getStringValue.getFirstInsn()) {
			HandlerFactory.messageHandler().handle(this, tstate, ti, insn);
		}
		else if (insn instanceof SwitchInstruction && tstate.considerSwitchInstruction((SwitchInstruction) insn)) {
			tstate.setSwitchInsn((SwitchInstruction) insn);
		}
		else if (insn instanceof IfInstruction && tstate.considerIfInstruction((IfInstruction) insn)) {
			HandlerFactory.ifThenElseHandler().handle(this, tstate, ti, insn);
		}
		else if (
				(Session_sendIfAllowed!=null && insn==Session_sendIfAllowed.getFirstInsn()) ||
				(Session_sendIfAllowedInt!=null && insn==Session_sendIfAllowedInt.getFirstInsn()) ||
				(Session_sendIfAllowedString!=null && insn==Session_sendIfAllowedString.getFirstInsn())
				) {
			HandlerFactory.sendIfAllowedHandler().handle(this, tstate, ti, insn);
		}
		else if(Participant_setConnection!=null && insn==Participant_setConnection.getFirstInsn()) {
			HandlerFactory.setConnectionHandler().handle(this, tstate, ti, insn);
		}
		else if (LoggerFactory_getLogger!=null && insn==LoggerFactory_getLogger.getFirstInsn()) {
			HandlerFactory.loggerFactoryHandler().handle(this, tstate, ti, insn);
		}
		else if (MultipleSessionReceiver_waitForReceive!=null && insn==MultipleSessionReceiver_waitForReceive.getFirstInsn()) {
			HandlerFactory.multipleSessionReceiverHandler().handle(this, tstate, ti, insn);
		}
		else if (methodsToSkip.contains(insn.getMethodInfo()) && insn == insn.getMethodInfo().getFirstInsn()) {
			HandlerFactory.skipMethodHandler().handle(this, tstate, ti, insn);
		}
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
			HandlerFactory.parallelEnteredHandler().handle(this, tstate, currentThread, enteredMethod);
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
						String sessionName = getSessionIDByPublic(pbl);
						
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
					String sessionName = getSessionIDByPublic(pbl);
					
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
	

	public static String insnToString(Instruction insn) {
		return insn.getPosition() + " - " + insn.getMnemonic() + " ("+insn.getMethodInfo().getFullName()+")";
	}
	
	/**
	 * Get the actual parameters of the given thread
	 * @param currentThread
	 * @return
	 */
	public Object[] getArguments(ThreadInfo currentThread) {
		return currentThread.getTopFrame().getArgumentValues(currentThread);
	}
	
	public ElementInfo getArgumentElementInfo(ThreadInfo currentThread, int position) {
		ElementInfo eiArgument = (ElementInfo) getArguments(currentThread)[position];
		return eiArgument;
	}
	
	public String getArgumentString(ThreadInfo currentThread, int position) {
		return getArgumentElementInfo(currentThread, position).asString();
	}
	
	public Integer getArgumentInteger(ThreadInfo currentThread, int position) {
		return (Integer) getArgumentElementInfo(currentThread, position).asBoxObject();
	}
	
	public int getArgumentInt(ThreadInfo currentThread, int position) {
		return (int) getArguments(currentThread)[position];
	}
	
	public List<ElementInfo> getArgumentArray(ThreadInfo currentThread, int position) {
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
	
	public List<String> getArgumentStringArray(ThreadInfo currentThread, int position) {
		
		List<String> strings = new ArrayList<String>();
		
		for (ElementInfo ei : getArgumentArray(currentThread, position)) {
			strings.add(ei.asString());
		}
		
		return strings;
	}
	
	public List<ElementInfo> getAllArgumentsAsElementInfo(ThreadInfo currentThread) {
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
	
	
	
	
	/*
	 * new public API (used by handlers)
	 */
	
	public void associateContractToSession(String contractID, String sessionID) {
		contractSessionMap.put(contractID, sessionID);
	}
	
	public void setContractDelay(String contractID, boolean hasDelay) {
		contractsDelay.put(contractID, hasDelay);
	}
	
	public boolean contractHasDelay(String contractID) {
		return contractsDelay.get(contractID);
	}
	
	public String getSessionID(String contractID) {
		return contractSessionMap.get(contractID);
	}
	
	public void saveContract(String contractID, ContractDefinition cDef) {
		contracts.put(cDef.getName(), cDef);
		
		log.info("adding contract: "+cDef.getName());
		
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
		
		sessions.add(getSessionID(contractID));
	}
	
	
	public String getActionValue(String contractID, String action) {
		
		Sort<?> sort = contractActionsSort.get(contractID).get(action);
		
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
	
	public String getSessionIDBySession(ThreadInfo ti, ElementInfo session) {
		ElementInfo pbl = ti.getElementInfo(session.getReferenceField("contract"));
		return getSessionIDByPublic(pbl);
	}
	
	private String getSessionIDByPublic(ElementInfo pbl) {
		String sessionName = contractSessionMap.get(getContractIDByPublic(pbl));
		assert sessionName!=null;
		return sessionName;
	}
	
	public String getContractIDBySession(ThreadInfo ti, ElementInfo session) {
		ElementInfo pbl = ti.getElementInfo(session.getReferenceField("contract"));
		return getContractIDByPublic(pbl);
	}
	
	public String getContractIDByPublic(ElementInfo pbl) {
		return pbl.getStringField("uniqueID");
	}


	public ThreadState getThreadState(ThreadInfo ti) {
		return threadStates.get(ti);
	}

	public void setCurrentThreadProcess(SumDS threadCurrentProcess) {
		this.threadCurrentProcess = threadCurrentProcess;
	}
	
	public void setCurrentThreadPrefix(PrefixPlaceholderDS threadCurrentPrefix) {
		this.threadCurrentPrefix = threadCurrentPrefix;
	}
	
}
