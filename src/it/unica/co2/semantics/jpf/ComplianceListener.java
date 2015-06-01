package it.unica.co2.semantics.jpf;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.VM;

public class ComplianceListener extends PropertyListenerAdapter {

	@Override
	public boolean check(Search search, VM vm) {
		// return false if property is violated
		return true;
	}

	@Override
	public void propertyViolated(Search search) {
		
	}
}
