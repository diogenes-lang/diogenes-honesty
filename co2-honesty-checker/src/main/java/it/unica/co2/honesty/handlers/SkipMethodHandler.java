package it.unica.co2.honesty.handlers;

import java.util.Arrays;
import java.util.stream.Stream;

import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.choice.IntChoiceFromList;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;


class SkipMethodHandler extends InstructionHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn) {

		log.info("");
		log.info("SKIPPING METHOD: "+insn.getMethodInfo().getFullName());
		

		AnnotationInfo ai = insn.getMethodInfo().getAnnotation(SkipMethod.class.getName());

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

}
