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
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import it.unica.co2.api.Session2;
import it.unica.co2.honesty.dto.DoReceiveDTO;
import it.unica.co2.honesty.dto.DoSendDTO;
import it.unica.co2.honesty.dto.IfThenElseDTO;
import it.unica.co2.honesty.dto.PrefixDTO;
import it.unica.co2.honesty.dto.ProcessDTO;
import it.unica.co2.honesty.dto.SumDTO;
import it.unica.co2.honesty.dto.TauDTO;
import it.unica.co2.honesty.dto.TellDTO;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import it.unica.co2.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
		
		this.processClass = processClass;
	}
	
	/*
	 * the class of the project to analyze
	 */
	private final Class<? extends Participant> processClass;
	
	/*
	 * the process we want to build up
	 */
	private ProcessDTO co2Process;
	
	/*
	 * D: where do I append the next Process? 
	 * A: co2CurrentPrefix.next
	 */
	private PrefixDTO co2CurrentPrefix;
	
	/*
	 * all told contracts
	 */
	private Map<String, Contract> contracts = new HashMap<>();
	private int contractCount=0;
	
	private List<String> sessions = new ArrayList<>();	// all sessions
	private Set<String> allActions = new HashSet<>();	// all actions
	
	/*
	 * store the entry-points of if-the-else processes, 
	 * useful to set the co2CurrentPrefix before analyzing a branch
	 * (the key is the choice-generator id)
	 */
	private Map<String, TauDTO> taus = new HashMap<>();
	private int ifThenElseCount=0;
	
	private List<PrefixDTO> sumPrefixes;
	
	/*
	 * if the ifInstruction is into this interval, skip it (means that is related to switch-statement)
	 */
	private int startIfExcluded = -1;
	private int endIfExcluded = -1;
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		
//		if (ci!=null && processClass.getName().equals(ci.getName()) )
//			log.info(ci.getName()+" - insn - "+insn.getPosition()+": "+insn);
		
		if (insn instanceof SwitchInstruction && processClass.getName().equals(ci.getName())) {
			SwitchInstruction switchInsn = (SwitchInstruction) insn;
			
			log.info("SWITCH : setting start="+switchInsn.getPosition()+" , end="+switchInsn.getTarget());
			startIfExcluded = switchInsn.getPosition();		// where the switch starts
			endIfExcluded = switchInsn.getTarget();		// where the switch ends
		}
		
		
		if (
				insn instanceof IfInstruction && 
				processClass.getName().equals(ci.getName()) && // consider only if into the class under test
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
				
				if (co2Process==null) {	// you are the first process
					co2Process = ifThenElse;
				}
				else {
					co2CurrentPrefix.next = ifThenElse;
				}
			
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
					co2CurrentPrefix = thenTau;
					ti.skipInstruction(ifInsn.getNext());
				}
				else {
					/*
					 * else branch
					 */
					log.info("setting tau, choice: "+myChoice);
					log.info("next insn: "+ifInsn.getTarget().getPosition());
					co2CurrentPrefix = elseTau;
					ti.skipInstruction(ifInsn.getTarget());
				}
				
				printInfo();
			}

		}
	}
	
	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
		if (exitedMethod.getBaseName().equals(Participant.class.getName()+".tell")) {
			log.info("");
			printInfo();
			log.info("--TELL-- (method exited)");
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			TellDTO tell = getTell(ei);
			SumDTO sum = new SumDTO();
			sum.prefixes.add(tell);
			
			if (co2Process==null) {	// you are the first process
				co2Process = sum;
				co2CurrentPrefix = tell;
			}
			else {
				co2CurrentPrefix.next = sum;
				co2CurrentPrefix = tell;
			}
			
			printInfo();
		}
		
		
		if (
				exitedMethod.getBaseName().equals(Session2.class.getName()+".waitForReceive")
				) {

			log.info("");
			printInfo();
			log.info("--SUM OF RECEIVE-- (method exited)");
			
			ElementInfo ei = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			/*
			 * get the returned value
			 */
			StackFrame f = currentThread.getTopFrame();
			
			int ref = f.getReferenceResult();	// apparently this remove the reference to the stackframe
			f.setReferenceResult(ref, null);	// re-put the reference on the stackframe
			
			ElementInfo retEi = currentThread.getElementInfo(ref);
			
			log.info(""+currentThread.getElementInfo(ref));
			String label = retEi.getStringField("label");
			String sessionName = ei.getStringField("sessionName");
			
			log.info("label: "+label);
			
			DoReceiveDTO received = null;
			for (PrefixDTO p : sumPrefixes) {
				
				if (((DoReceiveDTO) p).action.equals(label)) {
					received = ((DoReceiveDTO) p);
				}
			}
			
			if (received==null)
				throw new IllegalStateException("received cannot be null");
			
			received.action = label;
			received.session = sessionName;
			
			co2CurrentPrefix = received;
			
			
			printInfo();
		}
		
		if (
				exitedMethod.getBaseName().equals(Session2.class.getName()+".send")
				) {

			log.info("--SEND-- (exited method)");
		}
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		if (
				enteredMethod.getBaseName().equals(Session2.class.getName()+".waitForReceive")
				) {

			log.info("");
			printInfo();
			log.info("--SUM OF RECEIVE-- (entered method)");
			
			ElementInfo ei = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String session = ei.getStringField("sessionName");
			List<String> actions = getStringArrayArgument(currentThread);
			
			SumDTO sum = getSumOfReceive(session, actions);
			
			if (co2Process==null) {	// you are the first process
				co2Process = sum;
			}
			else {
				co2CurrentPrefix.next = sum;
			}
			
			/*
			 * the co2CurrentPrefix is set in methodExited
			 */
			sumPrefixes = sum.prefixes;
			
			printInfo();
		}
	
		if (
				enteredMethod.getBaseName().equals(Session2.class.getName()+".send")
				) {

			log.info("");
			printInfo();
			log.info("--SEND-- (entered method)");
			
			ElementInfo ei = currentThread.getThisElementInfo();	//the class that call waitForReceive(String...)
			
			String session = ei.getStringField("sessionName");
			String action = getFirstStringArgument(currentThread);
			
			this.allActions.add(action);
			
			DoSendDTO send = new DoSendDTO();
			send.session = session;
			send.action = action;
			
			SumDTO sum = new SumDTO();
			sum.prefixes.add(send);
			
			if (co2Process==null) {	// you are the first prefix
				co2Process = sum;
				co2CurrentPrefix = send;
			}
			else {
				co2CurrentPrefix.next = sum;
				co2CurrentPrefix = send;
			}

			printInfo();
		}
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		log.info("----------------NEW---------------: "+newCG.getId());
	}
	
	@Override
	public void stateBacktracked(Search search) {
		log.info("<<<<<<<<<<< BACKTRACK <<<<<<<<<<");
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
		log.info(">>>>>>>>>>> ADVANCE >>>>>>>>>>: "+currentCG.getId());
	}
	
	@Override
	public void searchFinished(Search search) {
		log.info("vvvvvvvvvvvvvvvvv SEARCH FINISHED vvvvvvvvvvvvvvvvvv");
		printInfo();
		
		log.info("contracts:");
		for (Entry<String, Contract> entry : contracts.entrySet()) {
			log.info("\t"+entry.getKey()+" --> "+entry.getValue().toMaude());
		}
		
		log.info("sessions: "+sessions);
	}
	
	
	
	//--------------------------------- UTILS -------------------------------
	
	private void printInfo() {
		if (co2Process==null || co2CurrentPrefix==null)
			return;
		
		log.info("process --> "+co2Process.toMaude());
		log.info("currentPrefix --> "+co2CurrentPrefix.toMaude());
	}
	
	private String getContractName() {
		return "C"+contractCount++;
	}
	
	private TellDTO getTell(ElementInfo ei) {
		
		String cName = getContractName();
		String sessionName = ei.getStringField("sessionName");
		contracts.put(cName, ObjectUtils.deserializeObjectFromStringQuietly(ei.getStringField("serializedContract"), Contract.class));
		sessions.add(sessionName);
		
		TellDTO tell = new TellDTO();
		tell.contractName = cName;
		tell.session = sessionName;
		
		return tell;
	}
	
	private SumDTO getSumOfReceive(String session, List<String> actions) {
		
		SumDTO sum = new SumDTO();
		
		for (String l : actions) {
			
			DoReceiveDTO p = new DoReceiveDTO(); 
			p.session = session;
			p.action = l;
			
			this.allActions.add(l);
			
			sum.prefixes.add(p);
		}
		
		return sum;
	}

	private String getFirstStringArgument(ThreadInfo currentThread) {
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		//get first argument: String
		ElementInfo eiArgument = (ElementInfo) f.getArgumentValues(currentThread)[0];
		
		return eiArgument.asString();
	}
	
	private List<String> getStringArrayArgument(ThreadInfo currentThread) {
		List<String> strings = new ArrayList<String>();
		
		//get stack frame
		StackFrame f = currentThread.getTopFrame();
		
		//get first argument: String[]
		ElementInfo eiArgument = (ElementInfo) f.getArgumentValues(currentThread)[0];
		ArrayFields af = eiArgument.getArrayFields();
		
		//iterate and collect the elements
		for (int i=0; i<af.arrayLength(); i++) {
			int ref = af.getReferenceValue(i);
			ElementInfo s = currentThread.getElementInfo(ref);
			
			strings.add(s.asString());
		}
		
		return strings;
	}
	
	
	
	//--------------------------------- GETTERS and SETTERS -------------------------------
	public ProcessDTO getCo2Process() {
		return co2Process;
	}
	
	public Map<String, Contract> getContracts() {
		return contracts;
	}
	
	public List<String> getSessions() {
		return sessions;
	}
	
	public Set<String> getActions() {
		return allActions;
	}
}
