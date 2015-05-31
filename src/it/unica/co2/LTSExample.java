package it.unica.co2;

import static it.unica.co2.model.Factory.externalAction;
import static it.unica.co2.model.Factory.externalSum;
import static it.unica.co2.model.Factory.internalAction;
import static it.unica.co2.model.Factory.internalSum;
import static it.unica.co2.model.Factory.recursion;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractConfiguration;
import it.unica.co2.semantics.LTS;

public class LTSExample {

	public static void main(String[] args) {
		
		
		Contract a = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction(
						"c",
						externalSum("a", "b")
				)
		);
		
		Recursion r = recursion();
		
		Contract b = externalSum(
				externalAction("a"), 
				externalAction("b"), 
				externalAction("c", r)
		);
		
		r.setContract(b);
		
		ContractConfiguration startState = new ContractConfiguration(a, r);
		
		
		
		LTS lts = new LTS(startState);

		lts.start(ContractConfiguration.class);

	}

}
