package it.unica.co2;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import org.junit.Test;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;


public class SyntaxGeneratorTest {

	@Test
	public void test1() {
		
		Contract A = internalSum().add("a").add("b");
		Contract B = externalSum().add("a").add("b").add("c");
		
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
		Contract A = internalSum()
				.add("a")
				.add("b")
				.add("c", ra)
		;
		ra.setContract(A);
		
		Recursion rb = recursion("rb");
		Contract B = externalSum()
				.add("a")
				.add("b", rb) 
				.add("c", rb)
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
