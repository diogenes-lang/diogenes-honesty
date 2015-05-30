package it.unica.co2;

import static it.unica.co2.model.Factory.*;
import it.unica.co2.model.Factory;
import it.unica.co2.model.contract.Contract;
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
		
		Contract b = Factory.externalSum("a", "b", "c");
		
		ContractConfiguration startState = new ContractConfiguration(a, b);
		
		
		
		LTS lts = new LTS(startState);

		lts.start(ContractConfiguration.class);

	}

}
