package it.unica.co2.honesty.handlers;

import java.util.logging.Level;

import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.vm.BooleanChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.IfThenElseDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;


class IfThenElseHandler extends AbstractHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

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
			tstate.printInfo();
			tstate.pushIfElse(ifThenElse);

			BooleanChoiceGenerator cg = new BooleanChoiceGenerator(tstate.getIfThenElseChoiceGeneratorName(), false);

			boolean cgSetOk = ti.getVM().getSystemState().setNextChoiceGenerator(cg);
			
			assert cgSetOk : "error setting the choice generator";
			
			ti.skipInstruction(insn);
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
				tstate.printInfo();
				
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
				tstate.printInfo();
				
				ti.skipInstruction(ifInsn.getTarget());
				
				tstate.setPeekElse();
				tstate.popIfElse();
			}
			
			tstate.printInfo(Level.FINER);
		}

	}

}
