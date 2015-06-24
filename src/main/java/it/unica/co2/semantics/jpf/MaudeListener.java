package it.unica.co2.semantics.jpf;

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
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import it.unica.co2.semantics.honesty.model.DoReceiveDTO;
import it.unica.co2.semantics.honesty.model.DoSendDTO;
import it.unica.co2.semantics.honesty.model.IfThenElseDTO;
import it.unica.co2.semantics.honesty.model.PrefixDTO;
import it.unica.co2.semantics.honesty.model.ProcessDTO;
import it.unica.co2.semantics.honesty.model.SumDTO;
import it.unica.co2.semantics.honesty.model.TauDTO;
import it.unica.co2.semantics.honesty.model.TellDTO;
import it.unica.co2.util.Facilities.Case;
import it.unica.co2.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MaudeListener extends ListenerAdapter {

	/*
	 * the process we want to build up
	 */
	ProcessDTO co2Process;
	
	/*
	 * D: where do I append the next Process? 
	 * A: co2CurrentPrefix.next
	 */
	PrefixDTO co2CurrentPrefix;
	
	/*
	 * all told contracts
	 */
	Map<String, Contract> contracts = new HashMap<>();
	int contractCount=0;
	
	
	/*
	 * store the entry-points of if-the-else processes, 
	 * useful to set the co2CurrentPrefix before analyzing a branch
	 * (the key is the choice-generator id)
	 */
	Map<String, TauDTO> taus = new HashMap<>();
	int ifThenElseCount=0;
	
	List<PrefixDTO> sumPrefixes;
	int sumPrefixIndex = -1;
	
	
	@Override
	public void instructionExecuted(VM vm, ThreadInfo ti, Instruction nextInsn, Instruction insn) {

		ClassInfo ci = ti.getExecutingClassInfo();
		String target = vm.getConfig().getTarget();

		if (insn instanceof IfInstruction && target.equals(ci.getName())) {
			
			IfInstruction ifInsn = (IfInstruction) insn;
			
			if (!ti.isFirstStepInsn()) { // top half - first execution

				System.out.println();
				printInfo();
				System.out.println("--IF_THEN_ELSE--");
				
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
				
				System.out.println("thenTau: "+thenTau);
				System.out.println("elseTau: "+elseTau);
			}
			else {
				// bottom half - reexecution at the beginning of the next
				// transition
				BooleanChoiceGenerator cg = vm.getSystemState().getCurrentChoiceGenerator("ifThenElseCG_"+ifThenElseCount, BooleanChoiceGenerator.class);
				
				assert cg != null : "no 'ifThenElseCG' BooleanChoiceGenerator found";
				
				
				Boolean myChoice = cg.getNextChoice();
				
				
				TauDTO thenTau = taus.get("then_"+cg.getId());
				TauDTO elseTau = taus.get("else_"+cg.getId());
				
				System.out.println("thenTau: "+thenTau);
				System.out.println("elseTau: "+elseTau);
				
				if (myChoice){
					System.out.println("setting tau, choice: "+myChoice);
					co2CurrentPrefix = thenTau;
					nextInsn = ifInsn.getNext();
				}
				else {
					System.out.println("setting tau, choice: "+myChoice);
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
			System.out.println();
			printInfo();
			System.out.println("--TELL--");
			
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
			System.out.println();
			printInfo();
			System.out.println("--SEND--");

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
			System.out.println();
			printInfo();
			System.out.println("--SUM OF RECEIVE--");
			
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
				System.out.println();
				printInfo();
				System.out.println("--RECEIVE--");
				// now I know which action was received and can set the prefix
				
				co2CurrentPrefix = sumPrefixes.get(sumPrefixIndex);
				
				printInfo();
			}
			
		}
	}
	
	
	private void printInfo() {
		if (co2Process==null || co2CurrentPrefix==null)
			return;
		
		System.out.println("process --> "+co2Process.toMaude());
		System.out.println("currentPrefix --> "+co2CurrentPrefix.toMaude());
	}
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		
	}
	
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		System.out.println("----------------NEW---------------: "+newCG.getId());
	}
	
	@Override
	public void stateBacktracked(Search search) {
		System.out.println("<<<<<<<<<<< BACKTRACK <<<<<<<<<<");
	}

	@Override
	public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> processedCG) {
		System.out.println("............... PROCESSED ..............: "+processedCG.getId());
		if (processedCG.getId().equals("ifThenElseCG_"+ifThenElseCount)) {
			ifThenElseCount++;
		}
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		System.out.println(">>>>>>>>>>> ADVANCE >>>>>>>>>>: "+currentCG.getId());

		if (currentCG.getId().equals("verifyGetInt(II)")) {
			
			System.out.println("multiple receive occurs");
			sumPrefixIndex++;
			System.out.println("i: "+sumPrefixIndex);
			
			if (sumPrefixes!=null && sumPrefixIndex==sumPrefixes.size()) {
				System.out.println("reset to -1");
				sumPrefixIndex=-1;
			}
		}
	}
	
	
	
	@Override
	public void searchFinished(Search search) {
		System.out.println("vvvvvvvvvvvvvvvvv SEARCH FINISHED vvvvvvvvvvvvvvvvvv");
		printInfo();
		System.out.println("contracts:");
		for (Entry<String, Contract> entry : contracts.entrySet()) {
			System.out.println("\t"+entry.getKey()+" --> "+entry.getValue().toMaude());
		}
	}
	
	
	
	private String getContractName() {
		return "C"+contractCount++;
	}
	
	private TellDTO getTell(ElementInfo ei) {
		
		String cName = getContractName();
		contracts.put(cName, ObjectUtils.deserializeObjectFromStringQuietly(ei.getStringField("serializedContract"), Contract.class));
		
		TellDTO tell = new TellDTO();
		tell.contractName = cName;
		tell.session = ei.getStringField("sessionName");
		
		return tell;
	}
	
	private DoSendDTO getDoSend(ElementInfo ei) {
		
		DoSendDTO send = new DoSendDTO();
		send.action = ei.getStringField("action"); 
		send.session = ei.getStringField("sessionName");
		
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
			
			sum.prefixes.add(p);
		}
		
		return sum;
	}
}
