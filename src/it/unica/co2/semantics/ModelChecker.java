package it.unica.co2.semantics;

import static it.unica.co2.model.Factory.externalSum;
import static it.unica.co2.model.Factory.internalAction;
import static it.unica.co2.model.Factory.internalSum;
import it.unica.co2.model.Factory;
import it.unica.co2.model.contract.Contract;

public class ModelChecker {

//	private static final JPF jpf;
//	private static final ComplianceListener complianceListener;
//	
//	static {
//		Config conf = JPF.createConfig(new String[]{});
//		conf.setTarget(ModelChecker.class.getName());
//		
//		complianceListener = new ComplianceListener();
//		
//		jpf = new JPF(conf);
//		jpf.addListener(complianceListener);
//	}
	
	public static boolean compliant(Contract a, Contract b) {
		
		
		return false;
	}
	
	public static void main(String[] args) {

		Contract a = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction(
						"c",
						externalSum("a", "b")
				)
		);
		
		Contract b = Factory.externalSum("a", "d");
		
		ContractConfiguration startState = new ContractConfiguration(a, b);
		
		
		
		LTS lts = new LTS(startState);

		lts.start();
	}
}
