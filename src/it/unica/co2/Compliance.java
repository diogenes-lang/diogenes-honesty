package it.unica.co2;

import static it.unica.co2.model.Factory.externalAction;
import static it.unica.co2.model.Factory.externalSum;
import static it.unica.co2.model.Factory.internalAction;
import static it.unica.co2.model.Factory.internalSum;
import it.unica.co2.model.Factory;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractComplianceChecker;

public class Compliance {

	public static void main(String[] args) throws Exception {
		
		Recursion r = Factory.recursion();
		
		Contract a = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction(
						"c",
						r
				)
		);
		
		r.setContract(a);
		
		
		Contract b = externalSum(
				externalAction("a"),
				externalAction("b"), 
				externalAction("c")
		);

		
		boolean compliance = ContractComplianceChecker.compliance(a,b);
		
		System.out.println("contract a: "+a);
		System.out.println("contract b: "+b);
		System.out.println("compliance: "+compliance);
	}

}
