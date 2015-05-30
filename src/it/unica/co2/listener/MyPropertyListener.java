package it.unica.co2.listener;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public class MyPropertyListener extends PropertyListenerAdapter {
	
	@Override
	public void choiceGeneratorSet(VM vm, ChoiceGenerator<?> newCG) {
		
		System.out.println("\t++ choiceGeneratorSet()");
		System.out.println("\tid: "+newCG.getId());
		System.out.println("\tclass: "+newCG.getClass());
		System.out.println("\tchoice type: "+newCG.getChoiceType());
		System.out.println("\t-- choiceGeneratorSet()");
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		System.out.println("\t++ methodEntered()");
		System.out.println("\tname: "+enteredMethod.getName());
		System.out.println("\tfullName: "+enteredMethod.getFullName());
		System.out.println("\tsignature: "+enteredMethod.getSignature());
		System.out.println("\tmethod class name: "+enteredMethod.getClassName());
		System.out.println("\t-- methodEntered()");
	}

	@Override
	public void methodExited (VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {
		
	}
}
