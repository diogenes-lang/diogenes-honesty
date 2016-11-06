package it.unica.co2;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;

import org.junit.Test;

import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;


public class SyntaxGeneratorTest {

	@Test
	public void test1() {
		
		SessionType A = internalSum().add("a").add("b");
		SessionType B = externalSum().add("a").add("b").add("c");
		
		System.out.println("------------------");
		System.out.println("toString: "+A);
		System.out.println("toMaude: "+A.toMaude());
		System.out.println("toMiddleware: "+A.toTST());

		System.out.println("------------------");
		System.out.println("toString: "+B);
		System.out.println("toMaude: "+B.toMaude());
		System.out.println("toMiddleware: "+B.toTST());
	}
	
	@Test
	public void test2() {
		
		Recursion ra = recursion("ra");
		SessionType A = internalSum()
				.add("a")
				.add("b")
				.add("c", recRef(ra))
		;
		ra.setContract(A);
		
		Recursion rb = recursion("rb");
		SessionType B = externalSum()
				.add("a")
				.add("b", recRef(rb)) 
				.add("c", recRef(rb))
		;
		rb.setContract(B);
		
		System.out.println("------------------");
		System.out.println("toString: "+ra);
		System.out.println("toMaude: "+ra.toMaude());
		System.out.println("toMiddleware: "+ra.toTST());
		
		System.out.println("------------------");
		System.out.println("toString: "+rb);
		System.out.println("toMaude: "+rb.toMaude());
		System.out.println("toMiddleware: "+rb.toTST());
	}
}
