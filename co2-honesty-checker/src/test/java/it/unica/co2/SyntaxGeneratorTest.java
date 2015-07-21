package it.unica.co2;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;

import org.junit.Test;


public class SyntaxGeneratorTest {

	@Test
	public void test1() {
		
		Contract A = internalSum("a", "b");
		Contract B = externalSum("a", "b", "c");
		
		System.out.println("------------------");
		System.out.println("toString: "+A);
		System.out.println("toMaude: "+A.toMaude());
		System.out.println("toMiddleware: "+A.toMiddleware());

		System.out.println("------------------");
		System.out.println("toString: "+B);
		System.out.println("toMaude: "+B.toMaude());
		System.out.println("toMiddleware: "+B.toMiddleware());
	}
	
	@Test
	public void test2() {
		
		Recursion ra = recursion();
		Contract A = internalSum()
				.add("a")
				.add("b")
				.add("c", ra)
		;
		ra.setContract(A);
		
		Recursion rb = recursion();
		Contract B = externalSum()
				.add("a")
				.add("b", rb) 
				.add("c", rb)
		;
		rb.setContract(B);
		
		System.out.println("------------------");
		System.out.println("toString: "+ra);
		System.out.println("toMaude: "+ra.toMaude());
		System.out.println("toMiddleware: "+ra.toMiddleware());
		
		System.out.println("------------------");
		System.out.println("toString: "+rb);
		System.out.println("toMaude: "+rb.toMaude());
		System.out.println("toMiddleware: "+rb.toMiddleware());
	}
}
