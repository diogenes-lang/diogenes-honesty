package it.unica.co2.honesty;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ExceptionInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.InvokeInstruction;
import it.unica.co2.api.Session2;
import it.unica.co2.honesty.dto.DoReceiveDTO;
import it.unica.co2.honesty.dto.DoSendDTO;
import it.unica.co2.honesty.dto.IfThenElseDTO;
import it.unica.co2.honesty.dto.PrefixDTO;
import it.unica.co2.honesty.dto.ProcessDTO;
import it.unica.co2.honesty.dto.ProcessDefinitionDTO;
import it.unica.co2.honesty.dto.SumDTO;
import it.unica.co2.honesty.dto.TauDTO;
import it.unica.co2.honesty.dto.TellDTO;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;
import it.unica.co2.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaudeListener extends ListenerAdapter {
	
	private static Logger log = JPF.getLogger(MaudeListener.class.getName());
	
	public MaudeListener(Config conf, Class<? extends Participant> processClass) {
		
		if (conf.getBoolean("honesty.listener.log", false)) {
			log.setLevel(Level.ALL);
		}
		else {
			log.setLevel(Level.OFF);
		}
		
		this.processUnderTestClass = processClass;
	}
	
	/*
	 * the class of the project to analyze
	 */
	private final Class<? extends Participant> processUnderTestClass;
	
	/*
	 * we use a stack to trace the call between different process
	 * 
	 * <top-frame>.process			the process you are currently building up
	 * <top-frame>.prefix.next		where you append the next process
	 * 
	 */
	private Stack<CO2StackFrame> co2ProcessesStack = new Stack<CO2StackFrame>();
	
	/*
	 * all told contracts
	 */
	private Map<String, Contract> contracts = new HashMap<>();
	private int contractCount=0;
	
	private List<String> sessions = new ArrayList<>();	// all sessions of the process under test
	
	/*
	 * store the entry-points of if-the-else processes, 
	 * useful to set the co2CurrentPrefix before analyzing a branch
	 * (the key is the choice-generator id)
	 */
	private Map<String, TauDTO> taus = new HashMap<>();
	private int ifThenElseCount=0;
	
	private Stack<SumStackFrame> sumStack = new Stack<SumStackFrame>();
	
	/*
	 * if the ifInstruction is into this interval, skip it (means that is related to switch-statement)
	 */
	private int startIfExcluded = -1;
	private int endIfExcluded = -1;
	
	/*
	 * env
	 */
	private Map<String, ProcessDefinitionDTO> envProcesses = new HashMap<>();
	private int envProcessesCount=0;
	
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		
//		if (insn instanceof InvokeInstruction && ci.getName().startsWith("it.unica.co2.examples"))
//			log.info(ci.getName()+" INVOKE insn - "+insn.getPosition()+": "+insn);
		
		if (
				insn instanceof InvokeInstruction && 
				ci.isInstanceOf(CO2Process.class.getName()) 
				) {
			
			InvokeInstruction invokeInsn = (InvokeInstruction) insn;
			
			MethodInfo invokedMethod = invokeInsn.getInvokedMethod();
			ClassInfo invokedClass = invokedMethod.getClassInfo();
//			ClassInfo callerClass = ci;
			
			if (
					invokedMethod.getName().equals("run") &&
					envProcesses.containsKey(invokedClass.getName())
					) {

//				log.info("INVOKE : caller "+callerClass.getName()+" --> "+invokedClass.getName()+"#"+invokedMethod.getName());
				
				ProcessDefinitionDTO proc = envProcesses.get(invokedClass.getName());
				
				ProcessDefinitionDTO processCall = (ProcessDefinitionDTO) proc.copy();
				processCall.isDefinition = false;
				
				setCurrentProcess(processCall);
				setCurrentPrefix(null);
				
				if (checkForRecursion(proc)) {
					/*
					 * the call is recursive: stop search
					 */
					log.info("recursive call detected, terminating");
					Instruction nextInsn = invokeInsn.getNext();
					
//					log.info("INVOKE INSN - "+invokeInsn.getPosition()+": "+invokeInsn);
//					log.info("INVOKE NEXT - "+nextInsn.getPosition()+": "+nextInsn);
					
					ti.skipInstruction(nextInsn);
				}
				else {
					log.info("NOT recursive call");
					
					CO2StackFrame frame = new CO2StackFrame();
					frame.prefix = proc.firstPrefix;
					frame.process = proc;
					frame.id = co2ProcessesStack.peek().id+1;
					
					log.info("adding processCall onto the stack");
					this.co2ProcessesStack.push(frame);
					
					log.info(proc.toMaude());
				}
			}
			
		}
		
		
		if (
				insn instanceof SwitchInstruction && 
				ci.isInstanceOf(CO2Process.class.getName())
				) {
			SwitchInstruction switchInsn = (SwitchInstruction) insn;
			/*
			 * switch statements use if-instructions that must be not considered for branch exploration
			 */
			log.info("SWITCH : setting start="+switchInsn.getPosition()+" , end="+switchInsn.getTarget());
			startIfExcluded = switchInsn.getPosition();		// where the switch starts
			endIfExcluded = switchInsn.getTarget();			// where the switch ends
		}
		
		
		if (
				insn instanceof IfInstruction && 
				ci.isInstanceOf(CO2Process.class.getName()) && 			// consider only if into the class under test
				!ci.getName().equals(Participant.class.getName()) && 	// ignore if instructions into Participant.class
				(insn.getPosition()<startIfExcluded || insn.getPosition()>endIfExcluded) // skip if instructions related to switch statements
				) {
			
			IfInstruction ifInsn = (IfInstruction) insn;
			
			if (!ti.isFirstStepInsn()) { // top half - first execution

				log.info("TOP HALF");
				
				log.info("");
				printInfo();
				log.info("--IF_THEN_ELSE--");
				
				IfThenElseDTO ifThenElse = new IfThenElseDTO();
				
				SumDTO thenStmt = new SumDTO();
				TauDTO thenTau = new TauDTO();
				thenStmt.prefixes.add(thenTau);
				
				SumDTO elseStmt = new SumDTO();
				TauDTO elseTau = new TauDTO();
				elseStmt.prefixes.add(elseTau);
				
				
				ifThenElse.thenStmt = thenStmt;
				ifThenElse.elseStmt = elseStmt;
				
				
				setCurrentProcess(ifThenElse);		//set the current process
				
				ifThenElseCount++;		// the counter is reset when the choice generator is PROCESSED
				BooleanChoiceGenerator cg = new BooleanChoiceGenerator("ifThenElseCG_"+ifThenElseCount, false);

				taus.put("then_"+cg.getId(), thenTau);
				taus.put("else_"+cg.getId(), elseTau);
				
				vm.getSystemState().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				
			}
			else {

				log.info("BOTTOM HALF");
				
				// bottom half - reexecution at the beginning of the next
				// transition
				BooleanChoiceGenerator cg = vm.getSystemState().getCurrentChoiceGenerator("ifThenElseCG_"+ifThenElseCount, BooleanChoiceGenerator.class);

				assert cg != null : "no 'ifThenElseCG' BooleanChoiceGenerator found";
				
				
				ifInsn.popConditionValue(ti.getModifiableTopFrame());		//remove operands from the stack
				
				Boolean myChoice = cg.getNextChoice();
				
				
				TauDTO thenTau = taus.get("then_"+cg.getId());
				TauDTO elseTau = taus.get("else_"+cg.getId());
				
				log.info("thenTau: "+thenTau);
				log.info("elseTau: "+elseTau);
				
				
				if (myChoice){
					/*
					 * then branch
					 */
					log.info("setting tau, choice: "+myChoice);
					log.info("next insn: "+ifInsn.getNext().getPosition());
					
					setCurrentPrefix(thenTau);		//set the current prefix

					ti.skipInstruction(ifInsn.getNext());
				}
				else {
					/*
					 * else branch
					 */
					log.info("setting tau, choice: "+myChoice);
					log.info("next insn: "+ifInsn.getTarget().getPosition());

					setCurrentPrefix(elseTau);		//set the current prefix
					
					ti.skipInstruction(ifInsn.getTarget());
				}
				
				printInfo();
			}

		}

	}
	
	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		
		if (
				exitedMethod.getBaseName().equals(Participant.class.getName()+".tell") &&
				exitedMethod.getSignature().equals("(Lit/unica/co2/model/contract/Contract;)Lco2api/Public;")
				) {
			log.info("");
			printInfo();
			log.info("--TELL-- (method exited)");
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDTO sum = sumStack.peek().sum;		//the sum of possible choice (set by entered-method)
			
			if (ex!=null) {
				
				assert false: "you should not be here";
				
				log.info("TIME_EXPIRED: "+ex.getExceptionClassname());
				
				assert sum.prefixes.size()>1;
				assert sum.prefixes.get(1) instanceof TauDTO;
				
				TauDTO p = (TauDTO) sum.prefixes.get(1);
				
				popSum(p);
				setCurrentPrefix(p);
			}
			else {
				assert sum.prefixes.size()>0;
				assert sum.prefixes.get(0) instanceof TellDTO;
				
				TellDTO tell = (TellDTO) sum.prefixes.get(0);
				
				ElementInfo ei = currentThread.getThisElementInfo();
				
				String cName = getContractName();
				String sessionName = ei.getStringField("sessionName");
				contracts.put(cName, ObjectUtils.deserializeObjectFromStringQuietly(ei.getStringField("serializedContract"), Contract.class));
				sessions.add(sessionName);
				
				tell.contractName = cName;
				tell.session = sessionName;
				
				popSum(tell);
				setCurrentPrefix(tell);
			}

			printInfo();
		}
		
		
		if (
				exitedMethod.getBaseName().equals(Session2.class.getName()+".waitForReceive") &&
				exitedMethod.getSignature().equals("(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;")
				) {

			log.info("");
			printInfo();
			log.info("--SUM OF RECEIVE-- (method exited)");
			
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDTO sum = sumStack.peek().sum;		//the sum of possible choice (set by entered-method)
			
			if (ex!=null) {
				/* 
				 * if you were here, you have call waitForReceive(Integer,String...) with a timeout
				 * a prefix tau must be present at this time
				 */
				log.info("TIME_EXPIRED: "+ex.getExceptionClassname());
				
				assert sum.prefixes.size()>0;
				assert sum.prefixes.get(sum.prefixes.size()-1) instanceof TauDTO;
				
				TauDTO p = (TauDTO) sum.prefixes.get(sum.prefixes.size()-1);
				
				popSum(p);
				setCurrentPrefix(p);
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
				String sessionName = session2.getStringField("sessionName");
				
				log.info("label: "+label);
				
				DoReceiveDTO received = null;
				
				for (int i=0; i<sum.prefixes.size(); i++) {
					
					PrefixDTO p = sum.prefixes.get(i);
					
					if (
							(p instanceof DoReceiveDTO) && 
							((DoReceiveDTO) p).action.equals(label)
							) {
						received = ((DoReceiveDTO) sum.prefixes.get(i));
						break;
					}
					
				}
				
				if (received==null)
					throw new IllegalStateException("received cannot be null");
				
				received.action = label;
				received.session = sessionName;
				
				popSum(received);
				setCurrentPrefix(received);		//set the current prefix
			}
			
			printInfo();
		}
		
		
		if (
				exitedMethod.getName().equals("run") &&
				envProcesses.containsKey(ci.getName())
				) {
			/*
			 * the process is finished
			 */
			log.info("");
			printInfo();
			log.info("--RUN ENV PROCESS-- (method exited) -> "+ci.getSimpleName());
			
			if (checkPendingSum()) {
				log.info("there are some pending sum, not removing from stack");
			}
			else {
				log.info("removing process from stack");
				this.co2ProcessesStack.pop();
			}
			
			printInfo();
		}
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		
		if (
				enteredMethod.getBaseName().equals(Participant.class.getName()+".tell") &&
				enteredMethod.getSignature().equals("(Lit/unica/co2/model/contract/Contract;)Lco2api/Public;")
				) {
			log.info("");
			printInfo();
			log.info("--TELL-- (method entered)");
			
//			Integer timeout = getIntegerArgument(currentThread,1);
//			log.info("timeout: "+timeout);
			
			SumDTO sum = new SumDTO();
			sum.prefixes.add(new TellDTO());
			
//			if (timeout==-1) {
//				/* nothing to do */
//			}
//			else {
//				/* add tau prefix, used if TimeExpiredException */
//				sum.prefixes.add(new TauDTO());
//			}
			
			setCurrentProcess(sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			pushSum(sum);
			
			printInfo();
		}
		
		if (
				enteredMethod.getBaseName().equals(Session2.class.getName()+".waitForReceive")&&
				enteredMethod.getSignature().equals("(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;")
				) {

			log.info("");
			printInfo();
			log.info("--SUM OF RECEIVE-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String sessionName = session2.getStringField("sessionName");
			
			
			Integer timeout = getFirstIntegerArgument(currentThread);
			List<String> actions = getStringArrayArgument(currentThread);
			
			
			SumDTO sum = new SumDTO();
			
			for (String l : actions) {
				
				DoReceiveDTO p = new DoReceiveDTO(); 
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
				sum.prefixes.add(new TauDTO());
			}
			
			setCurrentProcess(sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			pushSum(sum);
			
			printInfo();
		}
	
		if (
				enteredMethod.getBaseName().equals(Session2.class.getName()+".send")
				) {

			log.info("");
			printInfo();
			log.info("--SEND-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String sessionName = session2.getStringField("sessionName");
			String action = getFirstStringArgument(currentThread);
			
			DoSendDTO send = new DoSendDTO();
			send.session = sessionName;
			send.action = action;
			
			SumDTO sum = new SumDTO();
			sum.prefixes.add(send);
			
			setCurrentProcess(sum);		//set the current process
			setCurrentPrefix(send);		//set the current prefix

			printInfo();
		}
		
		
		
		if (
				enteredMethod.getName().equals("<init>") &&
				ci.isInstanceOf(CO2Process.class.getName()) &&
				!ci.getName().equals(Participant.class.getName()) &&		//ignore super construction
				!ci.getName().equals(CO2Process.class.getName()) &&		//ignore super construction
				!ci.getName().equals(processUnderTestClass.getName())
				) {
			
			/*
			 * the main process has created a new process
			 */
			log.info("");
			log.info("--INIT-- (method entered) -> "+ci.getSimpleName());
			
			String className = ci.getName();
			
			if (envProcesses.containsKey(className)) {
				log.info("envProcess "+className+" already exists");
			}
			else {
				
				ProcessDefinitionDTO proc = new ProcessDefinitionDTO();
				proc.name = getEnvProcessName();
				proc.firstPrefix = new TauDTO();
				proc.process = new SumDTO(proc.firstPrefix);
				
				List<ElementInfo> args = getArguments(currentThread);
				
				for (ElementInfo ei : args) {
					log.info(ei.toString());
					
					if (ei.getClassInfo().getName().equals(Session2.class.getName())) {
						String sessionName = ei.getStringField("sessionName");
						proc.freeNames.add("\""+sessionName+"\"");
						log.info("arg: Session2 ("+sessionName+")");
					}
					else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
						log.info("arg: Number");
					}
					else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
						log.info("arg: String");
					}
				}
				
				// store the process for future retrieve (when another one1 call it)
				log.info("saving envProcess "+className);
				envProcesses.put(className, proc);
			}
		}
		
	}
	
	
	
	@Override
	public void objectCreated(VM vm, ThreadInfo currentThread, ElementInfo newObject) {
		
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		log.info("----------------NEW---------------: "+newCG.getId());
	}
	
	@Override
	public void stateBacktracked(Search search) {
		log.info("<<<<<<<<<< BACKTRACK <<<<<<<<<<");
	}

	@Override
	public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> processedCG) {
		log.info("............... PROCESSED ..............: "+processedCG.getId());
		if (processedCG.getId().equals("ifThenElseCG_"+ifThenElseCount)) {
			ifThenElseCount--;
		}
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		log.info(">>>>>>>>>> ADVANCE >>>>>>>>>>: "+currentCG.getId());
	}
	
	@Override
	public void searchFinished(Search search) {
		log.info("");
		log.info("vvvvvvvvvvvvvvvvv SEARCH FINISHED vvvvvvvvvvvvvvvvv");
		printInfo();
		
		log.info("contracts:");
		for (Entry<String, Contract> entry : contracts.entrySet()) {
			log.info("\t"+entry.getKey()+" --> "+entry.getValue().toMaude());
		}
		
		log.info("env processes:");
		for (Entry<String, ProcessDefinitionDTO> entry : envProcesses.entrySet()) {
			log.info("\t"+entry.getValue().toMaude());
		}
		
		log.info("sessions: "+sessions);
	}
	
	
	
	//--------------------------------- UTILS -------------------------------
	
	private boolean checkForRecursion(ProcessDefinitionDTO process) {
		
		for (CO2StackFrame frame : co2ProcessesStack) {
			if (
					frame.process instanceof ProcessDefinitionDTO &&
					process.name.equals( ((ProcessDefinitionDTO) frame.process).name )
					) {
				return true;
			}
		}
		
		return false;
	}
	
	private void setCurrentProcess(ProcessDTO p) {

		if (co2ProcessesStack.isEmpty()) {	// you are the first process
			
			CO2StackFrame frame = new CO2StackFrame();
			frame.process = p;
			frame.id=0;
			co2ProcessesStack.push(frame);
		}
		else {
			assert co2ProcessesStack.size()>0;
			
			co2ProcessesStack.peek().prefix.next=p;
		}
	}
	
	private void setCurrentPrefix(PrefixDTO p) {
		
		assert co2ProcessesStack.size()>0;
		
		co2ProcessesStack.peek().prefix = p;
	}
	
//	private ProcessDTO getCurrentProcess() {
//		return co2ProcessesStack.peek().process;
//	}
//	
//	private PrefixDTO getCurrentPrefix() {
//		return co2ProcessesStack.peek().prefix;
//	}
	
	private void printInfo() {
		
		log.info("stackSize: "+co2ProcessesStack.size());
		
		if (co2ProcessesStack.size()>0) {
			CO2StackFrame frame = co2ProcessesStack.peek();
			log.info("top-process --> "+frame.process.toMaude());
			log.info("top-currentPrefix --> "+(frame.prefix!=null? frame.prefix.toMaude():"null"));
		}
		
	}
	
	private String getContractName() {
		return "C"+contractCount++;
	}
	
	private String getEnvProcessName() {
		return "P"+envProcessesCount++;
	}
	
	private String getFirstStringArgument(ThreadInfo currentThread) {
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		//get first argument: String
		ElementInfo eiArgument = (ElementInfo) f.getArgumentValues(currentThread)[0];
		
		return eiArgument.asString();
	}
	
	private Integer getFirstIntegerArgument(ThreadInfo currentThread) {
		return getIntegerArgument(currentThread, 0);
	}
	
	private Integer getIntegerArgument(ThreadInfo currentThread, int position) {
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		//get first argument: String
		ElementInfo eiArgument = (ElementInfo) f.getArgumentValues(currentThread)[position];
		
		return (Integer) eiArgument.asBoxObject();
	}
	
	private List<String> getStringArrayArgument(ThreadInfo currentThread) {
		List<String> strings = new ArrayList<String>();
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		//get first argument: String[]
		ElementInfo eiArgument = (ElementInfo) f.getArgumentValues(currentThread)[1];
		ArrayFields af = eiArgument.getArrayFields();
		
		//iterate and collect the elements
		for (int i=0; i<af.arrayLength(); i++) {
			int ref = af.getReferenceValue(i);
			ElementInfo s = currentThread.getElementInfo(ref);
			
			strings.add(s.asString());
		}
		
		return strings;
	}
	
	private List<ElementInfo> getArguments(ThreadInfo currentThread) {
		List<ElementInfo> args = new ArrayList<ElementInfo>();
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		for (Object obj : f.getArgumentValues(currentThread)) {
			args.add((ElementInfo) obj);
		}
		
		return args;
	}
	
	private void pushSum(SumDTO sum) {
		
		Set<String> toReceive = new HashSet<>();
		
		for (PrefixDTO p : sum.prefixes) {
			
			if (p instanceof TauDTO) {
				if(!toReceive.add("t"))
					throw new IllegalStateException("the set already contain a tau");
			}
			else if (p instanceof TellDTO) {
				if(!toReceive.add("tell"))
					throw new IllegalStateException("the set already contain a tell");
			}
			else if (p instanceof DoReceiveDTO) {
				toReceive.add(((DoReceiveDTO) p).action);
			}
		}
		
		SumStackFrame frame = new SumStackFrame();
		frame.sum = sum;
		frame.toReceive = toReceive;
		frame.ownerProcessFrame = co2ProcessesStack.peek().id;
		
		sumStack.push(frame);
	}
	
	private void popSum(TauDTO prefix) {
		popSum("t");
	}
	
	private void popSum(TellDTO prefix) {
		popSum("tell");
	}
	
	private void popSum(DoReceiveDTO prefix) {
		popSum(prefix.action);
	}

	private void popSum(String prefix) {
		
		Set<String> prefixToReceive = sumStack.peek().toReceive;
	
		assert prefixToReceive.contains(prefix);
		
		prefixToReceive.remove(prefix);
		
		if (prefixToReceive.isEmpty())
			sumStack.pop();
	}
	
	private boolean checkPendingSum() {
		
		int currentProcessFrameId = co2ProcessesStack.peek().id;
		
		for (SumStackFrame frame : sumStack) {
			if (frame.ownerProcessFrame==currentProcessFrameId)
				return true;
		}
		
		return false;
	}
	
	//--------------------------------- GETTERS and SETTERS -------------------------------
	public ProcessDTO getCo2Process() {
		return co2ProcessesStack.firstElement().process;
	}
	
	public Map<String, Contract> getContracts() {
		return contracts;
	}
	
	public List<String> getSessions() {
		return sessions;
	}
	
	public Collection<ProcessDefinitionDTO> getEnvProcesses() {
		return envProcesses.values();
	}
	
	
	//--------------------------------- UTILS CLASSES -------------------------------

	private static class CO2StackFrame {
		ProcessDTO process;		// the current process that you are building up
		PrefixDTO prefix;		// the current prefix (owned by the above process) where you append incoming events
		int id;
	}
	
	private static class SumStackFrame {
		SumDTO sum;
		Set<String> toReceive;
		int ownerProcessFrame;	//the process that own the sum
	}
	
}
