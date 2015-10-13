package it.unica.co2.honesty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import co2api.ContractExpiredException;
import co2api.TimeExpiredException;
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
	
	@Override
	public void classLoaded(VM vm, ClassInfo ci) {

		if (ci.getName().equals(Participant.class.getName())) {
			
			if (participantTell == null) {
				participantTell = ci.getMethod("tell", "(Lit/unica/co2/api/contract/ContractDefinition;Ljava/lang/Integer;)Lco2api/Public;", false); 
			}
			
			if (participantWaitForSession == null) {
				participantWaitForSession = ci.getMethod("waitForSession", "(Lco2api/Public;Ljava/lang/Integer;)Lit/unica/co2/api/Session2;", false); 
			}
		}
		
		if (ci.getName().equals(CO2Process.class.getName())) {
			if (parallel == null) {
				parallel = ci.getMethod("parallel", "(Ljava/lang/Runnable;)J", false); 
			}
		}
		
		if (ci.getName().equals(Session2.class.getName())) {
			if (sessionWaitForReceive==null) {
				sessionWaitForReceive = ci.getMethod("waitForReceive", "(Ljava/lang/Integer;[Ljava/lang/String;)Lco2api/Message;", false);
			}
			
			if (sessionSend==null) {
				sessionSend = ci.getMethod("send", "(Ljava/lang/String;)Ljava/lang/Boolean;", false);
			}
			
			if (sessionSendString==null) {
				sessionSendString = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Boolean;", false);
			}
			
			if (sessionSendInt==null) {
				sessionSendInt = ci.getMethod("send", "(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/Boolean;", false);
			}
		}
		
	}
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());

		if (
				insn instanceof InvokeInstruction && 
				ci.isInstanceOf(CO2Process.class.getName()) 
				) {
			
			InvokeInstruction invokeInsn = (InvokeInstruction) insn;
			
			MethodInfo invokedMethod = invokeInsn.getInvokedMethod();
			ClassInfo invokedClass = invokedMethod.getClassInfo();
			
			if (
					invokedMethod.getName().equals("run") &&
					envProcesses.containsKey(invokedClass.getName())
					) {
				
				ProcessDefinitionDS proc = envProcesses.get(invokedClass.getName());
				
				ProcessCallDS pCall = new ProcessCallDS();
				pCall.ref = proc;
				
				setCurrentProcess(tstate,pCall);
				setCurrentPrefix(tstate,null);
				
				boolean recursiveCall = checkForRecursion(tstate,proc);
				
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
					
					Instruction nextInsn = invokeInsn.getNext();
					
					ti.skipInstruction(nextInsn);	//skip the invoke
				}
				else {
					log.info("NOT recursive call");
					
					CO2StackFrame frame = new CO2StackFrame();
					frame.prefix = proc.firstPrefix;
					frame.process = proc;
					
					log.info("adding processCall onto the stack");
					tstate.co2ProcessesStack.push(frame);
					
					log.info(proc.toString());
				}
			}
			
		}
		else if (
				insn instanceof SwitchInstruction && 
				ci.isInstanceOf(CO2Process.class.getName())
				) {
			SwitchInstruction switchInsn = (SwitchInstruction) insn;
			/*
			 * switch statements use if-instructions that must be not considered for branch exploration
			 */
			log.info("");
			log.info("SWITCH : setting start="+switchInsn.getPosition()+" , end="+switchInsn.getTarget());
			log.info("methodIfExcluded: "+tstate.methodInfoIfExcluded);
			tstate.startIfExcluded = switchInsn.getPosition();		// where the switch starts
			tstate.endIfExcluded = switchInsn.getTarget();			// where the switch ends
			tstate.methodInfoIfExcluded = switchInsn.getMethodInfo();
		}
		else if (
				insn instanceof IfInstruction && 
				ci.isInstanceOf(CO2Process.class.getName()) && 			// consider only if into classes are instance of CO2Process
				insn.getMethodInfo().getName().equals("run") && 		// consider only if into the run method
				!ci.getName().equals(Participant.class.getName()) && 	// ignore if instructions into CO2Process.class
					(
						insn.getPosition()<tstate.startIfExcluded || 
						insn.getPosition()>tstate.endIfExcluded || 
						!insn.getMethodInfo().equals(tstate.methodInfoIfExcluded)
					)// skip if instructions related to switch statements
				) {
			
			IfInstruction ifInsn = (IfInstruction) insn;
			
			if (!ti.isFirstStepInsn()) { // top half - first execution

				log.info("TOP HALF");
				
				log.info("");
				printInfo(tstate);
				log.info("--IF_THEN_ELSE--");
				
				IfThenElseDS ifThenElse = new IfThenElseDS();
				ifThenElse.thenStmt = new PrefixPlaceholderDS();
				ifThenElse.elseStmt = new PrefixPlaceholderDS();
				
				
				setCurrentProcess(tstate,ifThenElse);		//set the current process
				pushIfElse(tstate, ifThenElse);

				BooleanChoiceGenerator cg = new BooleanChoiceGenerator("ifThenElseCG_"+tstate.co2ProcessesStack.peek().ifElseStack.size(), false);

				vm.getSystemState().setNextChoiceGenerator(cg);
				ti.skipInstruction(insn);
				
			}
			else {

				log.info("BOTTOM HALF");
				
				// bottom half - reexecution at the beginning of the next
				// transition
				BooleanChoiceGenerator cg = vm.getSystemState().getCurrentChoiceGenerator("ifThenElseCG_"+tstate.co2ProcessesStack.peek().ifElseStack.size(), BooleanChoiceGenerator.class);

				assert cg != null : "no 'ifThenElseCG' BooleanChoiceGenerator found";
				
				
				ifInsn.popConditionValue(ti.getModifiableTopFrame());		//remove operands from the stack
				
				Boolean myChoice = cg.getNextChoice();
				
				
				PrefixPlaceholderDS thenTau = tstate.co2ProcessesStack.peek().ifElseStack.peek().ifThenElse.thenStmt;
				PrefixPlaceholderDS elseTau = tstate.co2ProcessesStack.peek().ifElseStack.peek().ifThenElse.elseStmt;
				
				log.info("thenTau: "+thenTau);
				log.info("elseTau: "+elseTau);
				
				
				if (myChoice){
					/*
					 * then branch
					 */
					log.info("THEN branch, setting tau, choice: "+myChoice);
//					log.info("next insn: "+ifInsn.getNext().getPosition());
					
					setCurrentPrefix(tstate,thenTau);		//set the current prefix

					ti.skipInstruction(ifInsn.getNext());
					
					setPeekThen(tstate);
					popIfElse(tstate);
				}
				else {
					/*
					 * else branch
					 */
					log.info("ELSE branch, setting tau, choice: "+myChoice);
//					log.info("next insn: "+ifInsn.getTarget().getPosition());

					setCurrentPrefix(tstate,elseTau);		//set the current prefix
					
					ti.skipInstruction(ifInsn.getTarget());
					
					setPeekElse(tstate);
					popIfElse(tstate);
				}
				
				printInfo(tstate);
			}

		}

	}
	
	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());

		if (exitedMethod == participantTell) {
			log.info("");
			printInfo(tstate);
			log.info("--TELL-- (method exited)");
			
			TellDS tell = new TellDS();
			SumDS sum = new SumDS(tell);
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			String sessionName = ei.getStringField("sessionName");
			
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
			
			setCurrentProcess(tstate,sum);		//set the current process
			setCurrentPrefix(tstate,tell);

			printInfo(tstate);
		}
		else if (exitedMethod == participantWaitForSession) {

			log.info("");
			printInfo(tstate);
			log.info("--WAIT FOR SESSION-- (method exited)");
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDS sum = tstate.co2ProcessesStack.peek().sumStack.peek().sum;		//the sum of possible choice (set by entered-method)
			
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
					
					popSum(tstate,tau);
					setCurrentPrefix(tstate,tau);
				}
				else  if (ex.getExceptionClassname().equals(ContractExpiredException.class.getName())) {
					
					log.info("CONTRACT_EXPIRED: "+ex.getExceptionClassname());
					
					// the exception can be thrown by any waitForSession() (timeout or not)
					
					ElementInfo participantObj = currentThread.getThisElementInfo();
					String sessionName = participantObj.getStringField("sessionName");
					
					RetractDS retract = new RetractDS();
					retract.session = sessionName;
					
					sum.prefixes.add(retract);
					setCurrentPrefix(tstate, retract);
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
				
				String sessionName = session2.getStringField("sessionName");
				
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
				
				popSum(tstate,ask);
				setCurrentPrefix(tstate,ask);		//set the current prefix
			}
			
			printInfo(tstate);
		}
		else if (exitedMethod == sessionWaitForReceive) {

			log.info("");
			printInfo(tstate);
			log.info("--SUM OF RECEIVE-- (method exited)");
			
			
			ExceptionInfo ex = currentThread.getPendingException();			//get the (possible) pending exception
			
			SumDS sum = tstate.co2ProcessesStack.peek().sumStack.peek().sum;		//the sum of possible choice (set by entered-method)
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
				
				popSum(tstate,p);
				setCurrentPrefix(tstate,p);
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
				
				popSum(tstate,received);
				setCurrentPrefix(tstate,received);		//set the current prefix
			}
			
			printInfo(tstate);
		}
		else if (
				exitedMethod.getName().equals("run") &&
				envProcesses.containsKey(ci.getName())
				) {
			/*
			 * the process is finished
			 */
			log.info("");
			printInfo(tstate);
			log.info("--RUN ENV PROCESS-- (method exited) -> "+ci.getSimpleName());
			
			boolean pendingSum = !tstate.co2ProcessesStack.peek().sumStack.isEmpty();
			boolean pendingIfElse = !tstate.co2ProcessesStack.peek().ifElseStack.isEmpty();
			
			if (pendingSum || pendingIfElse) {
				
				if (pendingSum)
					log.info("there are some pending sum, not removing from stack");
				
				if (pendingIfElse)
					log.info("there are some pending if then else, not removing from stack");
			}
			else {
				log.info("removing process from stack");
				tstate.co2ProcessesStack.pop();
			}
			
			//next flag prevent from re-build the process at each invocation
			envProcesses.get(ci.getName()).alreadyBuilt = true;
			
			printInfo(tstate);
		}
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		ClassInfo ci = currentThread.getExecutingClassInfo();
		ThreadState tstate = threadStates.get(vm.getCurrentThread());
		
		if (enteredMethod == participantWaitForSession) {

			log.info("");
			printInfo(tstate);
			log.info("--WAIT FOR SESSION-- (entered method)");
			
			Integer timeout = getIntegerArgument(currentThread, 1);
			
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
			
			setCurrentProcess(tstate,sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			pushSum(tstate,sum);
			
			printInfo(tstate);
		}
		else if (enteredMethod == sessionWaitForReceive) {

			log.info("");
			printInfo(tstate);
			log.info("--SUM OF RECEIVE-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String sessionName = session2.getStringField("sessionName");
			
			
			Integer timeout = getFirstIntegerArgument(currentThread);
			List<String> actions = getStringArrayArgument(currentThread);
			
			
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
			
			setCurrentProcess(tstate,sum);		//set the current process

			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			log.info("pushing the sum onto the stack");
			pushSum(tstate,sum);
			
			printInfo(tstate);
		}
		else if (enteredMethod==sessionSend || enteredMethod==sessionSendInt || enteredMethod==sessionSendString) {

			log.info("");
			printInfo(tstate);
			log.info("--SEND-- (entered method)");
			
			ElementInfo session2 = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String sessionName = session2.getStringField("sessionName");
			String action = getFirstStringArgument(currentThread);
			
			DoSendDS send = new DoSendDS();
			send.session = sessionName;
			send.action = action;
			
			if (enteredMethod==sessionSend) send.sort = Sort.UNIT;
			if (enteredMethod==sessionSendInt) send.sort = Sort.INT;
			if (enteredMethod==sessionSendString) send.sort = Sort.STRING;
			
			SumDS sum = new SumDS();
			sum.prefixes.add(send);
			
			setCurrentProcess(tstate,sum);		//set the current process
			setCurrentPrefix(tstate,send);		//set the current prefix

			printInfo(tstate);
		}
		else if (
				enteredMethod.getName().equals("<init>") &&
				ci.isInstanceOf(CO2Process.class.getName()) &&
				!ci.getName().equals(Participant.class.getName()) &&	//ignore super construction
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
				
				ProcessDefinitionDS proc = new ProcessDefinitionDS();
				proc.name = ci.getSimpleName();
				proc.firstPrefix = new PrefixPlaceholderDS();
				proc.process = new SumDS(proc.firstPrefix);
				
				List<ElementInfo> args = getArguments(currentThread);
				
				if (args.size()==0) {
					//add at least one argument to make the process valid
					proc.freeNames.add("exp");
				}
				
				for (ElementInfo ei : args) {
					log.info(ei.toString());
					
					if (ei.getClassInfo().getName().equals(Session2.class.getName())) {
						String sessionName = ei.getStringField("sessionName");
						proc.freeNames.add("\""+sessionName+"\"");
						log.info("arg: Session2 ("+sessionName+")");
					}
					else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
						proc.freeNames.add("exp");
						log.info("arg: Number");
					}
					else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
						proc.freeNames.add("exp");
						log.info("arg: String");
					}
				}
				
				// store the process for future retrieve (when another one1 call it)
				log.info("saving envProcess "+className);
				envProcesses.put(className, proc);
				envProcessesList.add(proc);
			}
		}
		else if (enteredMethod==parallel) {
			log.info("");
			log.info("--PARALLEL-- (method entered) -> ID:"+tstate.threadInfo.getId());
			
			ParallelProcessesDS parallel = new ParallelProcessesDS();
			
			SumDS sumA = new SumDS();
			PrefixPlaceholderDS tauA = new PrefixPlaceholderDS();
			sumA.prefixes.add(tauA);
			
			SumDS sumB = new SumDS();
			PrefixPlaceholderDS tauB = new PrefixPlaceholderDS();
			sumB.prefixes.add(tauB);
			
			parallel.processA = sumA;
			parallel.processB = sumB;
			
			setCurrentProcess(tstate, parallel);
			setCurrentPrefix(tstate, tauB);
			
			threadCurrentProcess = sumA;
			threadCurrentPrefix = tauA;
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
		ThreadState tState = new ThreadState();
		tState.threadInfo = tInfo;
		
		threadStates.put(tInfo, tState);
		
		mainThread = tInfo;
	}
	
	
	@Override
	public void searchFinished(Search search) {
		log.info("");
		log.info("vvvvvvvvvvvvvvvvv SEARCH FINISHED vvvvvvvvvvvvvvvvv");
//		printInfo();
		
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
		
		ThreadState tState = new ThreadState();
		tState.threadInfo = startedThread;
		
		assert threadCurrentProcess!=null;
		assert threadCurrentPrefix!=null;
		
		setCurrentProcess(tState, threadCurrentProcess);
		setCurrentPrefix(tState, threadCurrentPrefix);
		
		threadStates.put(startedThread, tState);
	}
	
	//--------------------------------- UTILS -------------------------------
	
	private boolean checkForRecursion(ThreadState tstate, ProcessDefinitionDS process) {
		
		for (CO2StackFrame frame : tstate.co2ProcessesStack) {
			if (
					frame.process instanceof ProcessDefinitionDS &&
					process.name.equals( ((ProcessDefinitionDS) frame.process).name )
					) {
				return true;
			}
		}
		
		return false;
	}
	
	private void setCurrentProcess(ThreadState tstate, ProcessDS p) {

		if (tstate.co2ProcessesStack.isEmpty()) {	// you are the first process
			
			CO2StackFrame frame = new CO2StackFrame();
			frame.process = p;
			tstate.co2ProcessesStack.push(frame);
		}
		else {
			assert tstate.co2ProcessesStack.size()>0;
			
			tstate.co2ProcessesStack.peek().prefix.next=p;
		}
	}
	
	private void setCurrentPrefix(ThreadState tstate, PrefixDS p) {
		
		assert tstate.co2ProcessesStack.size()>0;
		
		tstate.co2ProcessesStack.peek().prefix = p;
	}
	
//	private ProcessDTO getCurrentProcess() {
//		return co2ProcessesStack.peek().process;
//	}
//	
//	private PrefixDTO getCurrentPrefix() {
//		return co2ProcessesStack.peek().prefix;
//	}
	
	private void printInfo(ThreadState tstate) {
		
		int thID = tstate.threadInfo.getId();
		
		log.info("[T-ID: "+thID+"] stackSize: "+tstate.co2ProcessesStack.size());
		
		if (tstate.co2ProcessesStack.size()>0) {
			CO2StackFrame frame = tstate.co2ProcessesStack.peek();
			log.info("[T-ID: "+thID+"] top-process --> "+frame.process.toString());
			log.info("[T-ID: "+thID+"] top-currentPrefix --> "+(frame.prefix!=null? frame.prefix.toString():"null"));
		}
		
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
	
	private void pushSum(ThreadState tstate, SumDS sum) {
		
		Set<String> toReceive = new HashSet<>();
		
		for (PrefixDS p : sum.prefixes) {
			
			if (p instanceof TauDS) {
				if(!toReceive.add("t"))
					throw new IllegalStateException("the set already contain a tau");
			}
			else if (p instanceof TellDS) {
				if(!toReceive.add("tell"))
					throw new IllegalStateException("the set already contain a tell");
			}
			else if (p instanceof AskDS) {
				if(!toReceive.add("ask"))
					throw new IllegalStateException("the set already contain an ask");
			}
			else if (p instanceof DoReceiveDS) {
				toReceive.add(((DoReceiveDS) p).action);
			}
		}
		
		SumStackFrame frame = new SumStackFrame();
		frame.sum = sum;
		frame.toReceive = toReceive;
		
		tstate.co2ProcessesStack.peek().sumStack.push(frame);
	}
	
	private void popSum(ThreadState tstate, TauDS prefix) {
		popSum(tstate,"t");
	}
	
	private void popSum(ThreadState tstate, AskDS prefix) {
		popSum(tstate,"ask");
	}
	
	private void popSum(ThreadState tstate, DoReceiveDS prefix) {
		popSum(tstate,prefix.action);
	}

	private void popSum(ThreadState tstate, String prefix) {
		
		Set<String> prefixToReceive = tstate.co2ProcessesStack.peek().sumStack.peek().toReceive;
	
		assert prefixToReceive.contains(prefix);
		
		prefixToReceive.remove(prefix);
		
		if (prefixToReceive.isEmpty())
			tstate.co2ProcessesStack.peek().sumStack.pop();
	}
	
	private void pushIfElse(ThreadState tstate, IfThenElseDS ifThenElse) {
		
		IfThenElseStackFrame frame = new IfThenElseStackFrame();
		frame.ifThenElse = ifThenElse;
		frame.thenStarted = false;
		frame.elseStarted = false;
		
		tstate.co2ProcessesStack.peek().ifElseStack.push(frame);
	}
	
	private void setPeekThen(ThreadState tstate) {
		tstate.co2ProcessesStack.peek().ifElseStack.peek().thenStarted = true;
	}
	
	private void setPeekElse(ThreadState tstate) {
		tstate.co2ProcessesStack.peek().ifElseStack.peek().elseStarted = true;
	}

	private void popIfElse(ThreadState tstate) {
		if (tstate.co2ProcessesStack.peek().ifElseStack.peek().thenStarted && tstate.co2ProcessesStack.peek().ifElseStack.peek().elseStarted) {
			tstate.co2ProcessesStack.peek().ifElseStack.pop();
		}
	}
	
	//--------------------------------- GETTERS and SETTERS -------------------------------
	public Class<? extends Participant> getProcessUnderTestClass() {
		return processUnderTestClass;
	}
	
	public ProcessDS getCo2Process() {
		return threadStates.get(mainThread).co2ProcessesStack.firstElement().process;
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
	
	
	//--------------------------------- UTILS CLASSES -------------------------------

	private static class CO2StackFrame {
		ProcessDS process;		// the current process that you are building up
		PrefixDS prefix;		// the current prefix (owned by the above process) where you append incoming events
		Stack<SumStackFrame> sumStack = new Stack<>(); 
		Stack<IfThenElseStackFrame> ifElseStack = new Stack<>();
	}
	
	private static class ChoiceStackFrame {}
	
	private static class SumStackFrame extends ChoiceStackFrame{
		SumDS sum;
		Set<String> toReceive;
	}
	
	private static class IfThenElseStackFrame extends ChoiceStackFrame{
		IfThenElseDS ifThenElse;
		boolean thenStarted;
		boolean elseStarted;
	}
	
	
	/**
	 * this class gather CO2 'execution informations' of a thread
	 */
	private static class ThreadState {
		
		ThreadInfo threadInfo;
		
		/*
		 * we use a stack to trace the call between different process
		 * 
		 * <top-frame>.process			the process you are currently building up
		 * <top-frame>.prefix.next		where you append the next process
		 * 
		 */
		Stack<CO2StackFrame> co2ProcessesStack = new Stack<CO2StackFrame>();
		
		/*
		 * if the ifInstruction is into this interval, skip it (means that is related to switch-statement)
		 */
		int startIfExcluded = -1;
		int endIfExcluded = -1;
		MethodInfo methodInfoIfExcluded;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((threadInfo == null) ? 0 : threadInfo.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ThreadState other = (ThreadState) obj;
			if (threadInfo == null) {
				if (other.threadInfo != null)
					return false;
			}
			else if (!threadInfo.equals(other.threadInfo))
				return false;
			return true;
		}
	}
	
}
