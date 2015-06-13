package it.unica.co2;

import static it.unica.co2.model.CO2Factory.*;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractConfiguration;
import it.unica.co2.semantics.lts.LTS;

public class LTSExample {

	public static void main(String[] args) {
		
		Recursion ra = recursion();
		Contract a = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction("c", ra)
		);
		ra.setContract(a);
		
		Recursion rb = recursion();
		Contract b = externalSum(
				externalAction("a"), 
				externalAction("b"), 
				externalAction("c", rb)
		);
		
		rb.setContract(b);
		
		ContractConfiguration startState = new ContractConfiguration(ra, rb);
		
		System.out.println("test:"+	"ciao".indexOf("c"));
		
		LTS lts = new LTS(startState);
		lts.start(ContractConfiguration.class);
	}

}
