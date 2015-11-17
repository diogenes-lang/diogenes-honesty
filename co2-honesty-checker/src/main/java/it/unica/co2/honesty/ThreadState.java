package it.unica.co2.honesty;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.SwitchInstruction;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.dto.CO2DataStructures.AskDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoReceiveDS;
import it.unica.co2.honesty.dto.CO2DataStructures.IfThenElseDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;

/**
 * this class gather CO2 'execution informations' of a thread
 */
class ThreadState {
	
	public static Logger logger = JPF.getLogger(ThreadState.class.getName());
	
	private ThreadInfo threadInfo;

	/*
	 * we use a stack to trace the call between different process
	 * 
	 * <top-frame>.process			the process you are currently building up
	 * <top-frame>.prefix.next		where you append the next process
	 * 
	 */
	private Stack<CO2StackFrame> co2ProcessesStack = new Stack<CO2StackFrame>();
	
	/*
	 * if the ifInstruction is to switch-statement, not consider it
	 */
	private SwitchInstruction switchInsn;
	
	
	
	
	public ThreadState(ThreadInfo threadInfo) {
		this.threadInfo = threadInfo;
	}
	
	
	public boolean isCurrentProcessCompleted() {
		return co2ProcessesStack.peek().process != null &&
				co2ProcessesStack.peek().prefix == null;
	}
	
	
	/**
	 * @return the id of the thread
	 */
	public int getId() {
		return threadInfo.getId();
	}
	
	/**
	 * Return a fresh name for a boolean choice generator.
	 * The freshness is necessary on nested if-then-else statements.
	 * @return a fresh name for a boolean choice generator.
	 */
	public String getBooleanChoiceGeneratorName () {
		return "ifThenElseCG_"+this.co2ProcessesStack.peek().ifElseStack.size();
	}
	
	/**
	 * 
	 * @return
	 */
	public PrefixPlaceholderDS getThenPlaceholder() {
		return co2ProcessesStack.peek().ifElseStack.peek().ifThenElse.thenStmt;
	}
	
	/**
	 * 
	 * @return
	 */
	public PrefixPlaceholderDS getElsePlaceholder() {
		return co2ProcessesStack.peek().ifElseStack.peek().ifThenElse.elseStmt;
	}
	
	/**
	 * 
	 * @return
	 */
	public SumDS getSum() {
		return co2ProcessesStack.peek().sumStack.peek().sum;
	}
	
	/**
	 * Set the switch instruction in order to skip its if-instructions.
	 * <p>
	 * The java switch-statement generates multiple bytecode instructions.
	 * These instructions can contain if-instructions, not related to java if-statements.
	 * So, because we want to create a boolean-choice-generator ONLY for if-instructions related to if-statements,
	 * we use the given <code>SwitchInstruction</code> to skip if-instructions related to switch-statements.
	 * </p>
	 * See {@link ThreadState#considerIfInstruction(IfInstruction)}.
	 * @param switchInsn
	 */
	public void setSwitchInsn(SwitchInstruction switchInsn) {
		this.switchInsn = switchInsn;
	}
	
	/**
	 * @param ifInsn
	 * @return true if the given <code>IfInstruction</code> has to generate a new boolean choice generator.
	 */
	public boolean considerIfInstruction(IfInstruction ifInsn) {

		MethodInfo mi = ifInsn.getMethodInfo();
		ClassInfo ci = mi.getClassInfo();
		
		return
				mi.getName().equals("run") && 									// consider only 'if' into the run method
				!ci.getName().equals(CO2Process.class.getName()) && 			// ignore 'if' instructions into Participant.class
				!ci.getName().equals(Participant.class.getName()) && 			// ignore 'if' instructions into Participant.class
				ci.isInstanceOf(CO2Process.class.getName()) && 					// consider only 'if' into classes are instance of CO2Process
			(																	// skip if instructions related to switch statements:
				switchInsn==null ||
				ifInsn.getPosition() < switchInsn.getPosition() || 				// - where the switch starts
				ifInsn.getPosition() > switchInsn.getTarget() || 				// - where the switch ends
				!mi.equals(switchInsn.getMethodInfo())
			);																	
	}
	
	/**
	 * @return the first process built by this thread.
	 */
	public ProcessDS getFirstProcess() {
		return co2ProcessesStack.firstElement().process;
	}
	
	/**
	 * @param processDefinition
	 * @return true if the given process definition is already present in a frame, false otherwise.
	 */
	public boolean checkForRecursion(ProcessDefinitionDS processDefinition) {
		
		for (CO2StackFrame frame : co2ProcessesStack) {
			if (
					frame.process instanceof ProcessDefinitionDS &&
					processDefinition.name.equals( ((ProcessDefinitionDS) frame.process).name )
					) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Set the given process as the current process.
	 * @param process
	 */
	public void setCurrentProcess(ProcessDS process) {

		if (co2ProcessesStack.isEmpty()) {	// you are the first process
			
			CO2StackFrame frame = new CO2StackFrame();
			frame.process = process;
			co2ProcessesStack.push(frame);
		}
		else {
			assert co2ProcessesStack.size()>0;
			
			//FIXME: the next code is a workaround (no good)
			if (co2ProcessesStack.peek().prefix!=null)
				co2ProcessesStack.peek().prefix.next=process;
			else
				logger.warning("the current prefix is 'null'. This is a known bug and should be resolved in the future.");
		}
	}
	
	/**
	 * Set the given prefix as the current prefix.
	 * @param prefix
	 */
	public void setCurrentPrefix(PrefixDS prefix) {
		assert co2ProcessesStack.size()>0;
		co2ProcessesStack.peek().prefix = prefix;
	}
	
	/**
	 * Print some informations about the stack.
	 */
	public void printInfo(Level level) {
		
		int thID = threadInfo.getId();
		
		logger.log(level, "[T-ID: "+thID+"] stackSize: "+co2ProcessesStack.size());
				
		if (co2ProcessesStack.size()>0) {
			CO2StackFrame frame = co2ProcessesStack.peek();
			logger.log(level, "[T-ID: "+thID+"] top-process --> "+frame.process.toString());
			logger.log(level, "[T-ID: "+thID+"] top-currentPrefix --> "+(frame.prefix!=null? frame.prefix.toString():"null"));
		}
		
	}
	
	public void printInfo() {
		printInfo(Level.INFO);
	}
	
	
	
	
	public void pushSum(SumDS sum) {
		
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
		
		co2ProcessesStack.peek().sumStack.push(frame);
	}
	
	public void popSum(TauDS prefix) {
		popSum("t");
	}
	
	public void popSum(AskDS prefix) {
		popSum("ask");
	}
	
	public void popSum(DoReceiveDS prefix) {
		popSum(prefix.action);
	}

	public void popSum(String prefix) {
		
		Set<String> prefixToReceive = co2ProcessesStack.peek().sumStack.peek().toReceive;
	
		assert prefixToReceive.contains(prefix);
		
		prefixToReceive.remove(prefix);
		
		if (prefixToReceive.isEmpty())
			co2ProcessesStack.peek().sumStack.pop();
	}
	
	public void pushIfElse(IfThenElseDS ifThenElse) {
		
		IfThenElseStackFrame frame = new IfThenElseStackFrame();
		frame.ifThenElse = ifThenElse;
		frame.thenStarted = false;
		frame.elseStarted = false;
		
		co2ProcessesStack.peek().ifElseStack.push(frame);
	}
	
	public void setPeekThen() {
		co2ProcessesStack.peek().ifElseStack.peek().thenStarted = true;
	}
	
	public void setPeekElse() {
		co2ProcessesStack.peek().ifElseStack.peek().elseStarted = true;
	}

	public void popIfElse() {
		if (co2ProcessesStack.peek().ifElseStack.peek().thenStarted && co2ProcessesStack.peek().ifElseStack.peek().elseStarted) {
			co2ProcessesStack.peek().ifElseStack.pop();
		}
	}
	
	
	public void pushNewFrame(ProcessDefinitionDS proc) {
		CO2StackFrame frame = new CO2StackFrame();
		frame.prefix = proc.firstPrefix;
		frame.process = proc;
		
		logger.info("adding processCall onto the stack");
		co2ProcessesStack.push(frame);
	}
	
	public void tryToPopFrame() {
		boolean pendingSum = !co2ProcessesStack.peek().sumStack.isEmpty();
		boolean pendingIfElse = !co2ProcessesStack.peek().ifElseStack.isEmpty();
		
		if (pendingSum || pendingIfElse) {
			
			if (pendingSum)
				logger.info("there are some pending sum, not removing from stack");
			
			if (pendingIfElse)
				logger.info("there are some pending if then else, not removing from stack");
		}
		else {
			logger.info("removing process from stack");
			co2ProcessesStack.pop();
		}
	}
	
	
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
	
	
	
	
	//--------------------------------- UTILS CLASSES -------------------------------

	static class CO2StackFrame {
		ProcessDS process;		// the current process that you are building up
		PrefixDS prefix;		// the current prefix (owned by the above process) where you append incoming events
		Stack<SumStackFrame> sumStack = new Stack<>(); 
		Stack<IfThenElseStackFrame> ifElseStack = new Stack<>();
	}
	
	private static class ChoiceStackFrame {}
	
	public static class SumStackFrame extends ChoiceStackFrame{
		SumDS sum;
		Set<String> toReceive;
	}
	
	public static class IfThenElseStackFrame extends ChoiceStackFrame{
		IfThenElseDS ifThenElse;
		boolean thenStarted;
		boolean elseStarted;
	}
}