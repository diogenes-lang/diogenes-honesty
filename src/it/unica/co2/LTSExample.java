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
