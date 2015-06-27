package it.unica.co2.examples;


import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.compliance.ContractConfiguration;
import it.unica.co2.lts.LTS;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;

public class LTSExample {

	public static void main(String[] args) {
		
		Recursion ra = recursion();
		Contract a = internalSum()
				.add("a")
				.add("b")
				.add("c", ra)
		;
		ra.setContract(a);
		
		Recursion rb = recursion();
		Contract b = externalSum()
				.add("a")
				.add("b")
				.add("c", rb)
		;
		
		rb.setContract(b);
		
		ContractConfiguration startState = new ContractConfiguration(ra, rb);
		
		System.out.println("test:"+	"ciao".indexOf("c"));
		
		LTS lts = new LTS(startState);
		lts.start(ContractConfiguration.class);
	}

}
