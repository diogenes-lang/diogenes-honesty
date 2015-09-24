package it.unica.co2;

import static it.unica.co2.api.contract.ContractFactory.*;
import static org.junit.Assert.*;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.compliance.ComplianceChecker;

import org.junit.Test;

public class ComplianceTest {

	@Test
	public void test1() {
	
		System.out.println("\n\n-- TEST 1 --");
		
		/*
		 * A = a! (+) b!
		 * B = a?  +  b?  +  c?
		 * 
		 * A |x| B = true
		 */
		Contract A = internalSum().add("a").add("b");
		Contract B = externalSum().add("a").add("b").add("c");
		assertTrue( ComplianceChecker.compliance(A, B) );
	}
	
	@Test
	public void test2() {
		
		System.out.println("\n\n-- TEST 2 --");
	
		/*
		 * A = rec X. a! (+) b! (+) c!.X
		 * B = rec X. a?  +  b?  + c?.X
		 * 
		 * A |x| B = true
		 */		
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
				.add("b")
				.add("c", rb)
		;
		rb.setContract(B);

		assertTrue( ComplianceChecker.compliance(ra, rb) );
	}
	
	@Test
	public void test3() {
		
		System.out.println("\n\n-- TEST 3 --");

		/*
		 * A = a! (+) b! (+) c!
		 * B = a?  +  b?
		 * 
		 * A |x| B = false
		 */		
		Contract A = internalSum().add("a").add("b").add("c");
		Contract B = externalSum().add("a").add("b");
		assertFalse( ComplianceChecker.compliance(A, B) );
	}
	
	@Test
	public void test4() {
		
		System.out.println("\n\n-- TEST 4 --");

		/*
		 * A = a! (+) b! . ( a?  +  c? . b! )
		 * B = a?  +  b? . ( a! (+) c! )
		 * 
		 * A |x| B = false
		 */		
		Contract A = internalSum()
				.add("a")
				.add("b", 
						externalSum()
						.add("a")
						.add("c", internalSum().add("b")))
		;
				
		Contract B = externalSum()
				.add("a")
				.add("b",
						internalSum().add("a").add("c")
				)
		;
		assertFalse( ComplianceChecker.compliance(A, B) );
	}

}
