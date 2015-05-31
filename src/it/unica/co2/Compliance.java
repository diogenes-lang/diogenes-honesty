package it.unica.co2;

import static it.unica.co2.model.Factory.externalAction;
import static it.unica.co2.model.Factory.externalSum;
import static it.unica.co2.model.Factory.internalAction;
import static it.unica.co2.model.Factory.internalSum;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.semantics.ModelChecker;

public class Compliance {

	public static void main(String[] args) {
		
		
		Contract a = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction(
						"c",
						externalSum("a", "b")
				)
		);
		
		Contract b = externalSum(
				externalAction("a"), 
				externalAction("b"), 
				externalAction("c")
		);

		
		boolean compliance = ModelChecker.compliant(a, b);
		
		System.out.println("contract a: "+a);
		System.out.println("contract b: "+b);
		System.out.println("compliance: "+compliance);
	}

}
