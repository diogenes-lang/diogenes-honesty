package it.unica.co2.honesty;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
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
import it.unica.co2.util.Facilities.Case;
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
	
	public MaudeListener(Config conf) {
		
		if (conf.getBoolean("honesty.listener.log", false)) {
			log.setLevel(Level.ALL);
		}
		else {
			log.setLevel(Level.OFF);
		}
	}
	
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
	private Set<String> actions = new HashSet<>();	// all actions
	
	/*
	 * store the entry-points of if-the-else processes, 
	 * useful to set the co2CurrentPrefix before analyzing a branch
	 * (the key is the choice-generator id)
	 */
	private Map<String, TauDTO> taus = new HashMap<>();
	private int ifThenElseCount=0;
	
	private List<PrefixDTO> sumPrefixes;
	private int sumPrefixIndex = -1;
	
	
	@Override
	public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		String target = vm.getConfig().getTarget();

		if (insn instanceof IfInstruction && target.equals(ci.getName())) {
			
			IfInstruction ifInsn = (IfInstruction) insn;
			
			if (!ti.isFirstStepInsn()) { // top half - first execution

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
			
				BooleanChoiceGenerator cg = new BooleanChoiceGenerator("ifThenElseCG_"+ifThenElseCount);
				vm.getSystemState().setNextChoiceGenerator(cg);
				ti.reExecuteInstruction();
				
				taus.put("then_"+cg.getId(), thenTau);
				taus.put("else_"+cg.getId(), elseTau);
				
				log.info("thenTau: "+thenTau);
				log.info("elseTau: "+elseTau);
			}
			else {
				// bottom half - reexecution at the beginning of the next
				// transition
				BooleanChoiceGenerator cg = vm.getSystemState().getCurrentChoiceGenerator("ifThenElseCG_"+ifThenElseCount, BooleanChoiceGenerator.class);
				
				assert cg != null : "no 'ifThenElseCG' BooleanChoiceGenerator found";
				
				
				Boolean myChoice = cg.getNextChoice();
				
				
				TauDTO thenTau = taus.get("then_"+cg.getId());
				TauDTO elseTau = taus.get("else_"+cg.getId());
				
				log.info("thenTau: "+thenTau);
				log.info("elseTau: "+elseTau);
				
				if (myChoice){
					log.info("setting tau, choice: "+myChoice);
					co2CurrentPrefix = thenTau;
					nextInsn = ifInsn.getNext();
				}
				else {
					log.info("setting tau, choice: "+myChoice);
					co2CurrentPrefix = elseTau;
					nextInsn = ifInsn.getTarget();
				}
				
				ti.setNextPC(nextInsn);
				
				
				printInfo();
			}

		}
	}
	
	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		if (enteredMethod.getBaseName().equals(Participant.class.getName()+".tell")) {
			log.info("");
			printInfo();
			log.info("--TELL--");
			
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
		
		if (enteredMethod.getBaseName().equals(Session2.class.getName()+".send")) {
			log.info("");
			printInfo();
			log.info("--SEND--");

			ElementInfo ei = currentThread.getThisElementInfo();
			
			DoSendDTO send = getDoSend(ei);
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
		
		if (
				enteredMethod.getBaseName().equals(Session2.class.getName()+".waitForReceive") && 
				sumPrefixIndex ==0
				) {
			log.info("");
			printInfo();
			log.info("--SUM OF RECEIVE--");
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			SumDTO sum = getSumOfReceive(ei);
			
			if (co2Process==null) {	// you are the first process
				co2Process = sum;
			}
			else {
				co2CurrentPrefix.next = sum;
			}
			/*
			 * at this point you dont'n know which branch will be chosen (see next method)
			 */
			sumPrefixes = sum.prefixes;

			printInfo();
		}
		
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
	
		if (enteredMethod.getBaseName().equals(Case.class.getName()+".runCase")) {
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			String actionName = ei.getStringField("actionName");
			boolean received = actionName!=null;
			
			
			if (received) {
				log.info("");
				printInfo();
				log.info("--RECEIVE--");
				// now I know which action was received and can set the prefix
				
				co2CurrentPrefix = sumPrefixes.get(sumPrefixIndex);
				
				printInfo();
			}
			
		}
	}
	
	
	private void printInfo() {
		if (co2Process==null || co2CurrentPrefix==null)
			return;
		
		log.info("process --> "+co2Process.toMaude());
		log.info("currentPrefix --> "+co2CurrentPrefix.toMaude());
	}
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		
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
			ifThenElseCount++;
		}
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		log.info(">>>>>>>>>>> ADVANCE >>>>>>>>>>: "+currentCG.getId());

		if (currentCG.getId().equals("verifyGetInt(II)")) {
			
			log.info("multiple receive occurs");
			sumPrefixIndex++;
			log.info("i: "+sumPrefixIndex);
			
			if (sumPrefixes!=null && sumPrefixIndex==sumPrefixes.size()) {
				log.info("reset to -1");
				sumPrefixIndex=-1;
			}
		}
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
	
	private DoSendDTO getDoSend(ElementInfo ei) {
		
		DoSendDTO send = new DoSendDTO();
		send.action = ei.getStringField("action"); 
		send.session = ei.getStringField("sessionName");
		
		actions.add(send.action);
		
		return send;
	}
	
	private SumDTO getSumOfReceive(ElementInfo ei) {
		
		SumDTO sum = new SumDTO();
		
		String session = ei.getStringField("sessionName");
		String[] labels = ObjectUtils.deserializeObjectFromStringQuietly(
				ei.getStringField("labels"), 
				String[].class
				);
		
		for (String l : labels) {
			
			DoReceiveDTO p = new DoReceiveDTO(); 
			p.session = session;
			p.action = l;
			
			actions.add(l);
			
			sum.prefixes.add(p);
		}
		
		return sum;
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
		return actions;
	}
}
