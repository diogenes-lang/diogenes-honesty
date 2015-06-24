package it.unica.co2.semantics.jpf;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.semantics.ContextAbstractContractConfiguration;
import it.unica.co2.util.ObjectUtils;

public class HonestyListener extends PropertyListenerAdapter {

	static JPFLogger log = JPF.getLogger(HonestyListener.class.getName());

	String msg = null;

	ContextAbstractContractConfiguration ctxAbsContractConf;
	
	@Override
	public boolean check(Search search, VM vm) {
		return msg==null;
	}

	@Override
	public String getErrorMessage() {
		return msg;
	}
	
	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		
		if (
				enteredMethod.getName().equals("run")
				) {
			
			System.out.println(enteredMethod);
			
			ElementInfo ei = currentThread.getThisElementInfo();
			
			String serializedContract = ei.getStringField("serializedContract");
			
			Contract contract = ObjectUtils.deserializeObjectFromStringQuietly(serializedContract, Contract.class);
			
			this.ctxAbsContractConf = ContextAbstractContractConfiguration.getInstance(contract);
			
			System.out.println(serializedContract);
			System.out.println(contract);
		}
	}
	
}
