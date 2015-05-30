package it.unica.co2.semantics.jpf;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import it.unica.co2.semantics.ContractConfiguration;

public class ComplianceListener extends PropertyListenerAdapter {

	
//	@Override
//	public boolean check(Search search, VM vm) {
//		// return false if property is violated
//		return true;
//	}
//	
//	@Override
//	public void methodEntered (VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
//		
////		if (
////				enteredMethod.getClassName().equals(ContractConfiguration.class.getCanonicalName()) &&
////				enteredMethod.getName().equals("hasNext")
////				) {
////			
////			System.out.println("called method ContractConfiguration#hasNext()");
////			
////		}
//	}
//	
//	@Override
//	public void objectCreated(VM vm, ThreadInfo currentThread, ElementInfo newObject) {
////		if (
////				newObject.getClassInfo().getName().equals(ContractConfiguration.class.getCanonicalName())
////				) {
////			System.out.println("created ContractConfiguration");
////		}
//	}
}
