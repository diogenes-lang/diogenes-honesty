package it.unica.co2;

import static it.unica.co2.model.CO2Factory.*;
import static org.junit.Assert.*;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractComplianceChecker;

import org.junit.Test;

public class ComplianceTest {

	@Test
	public void test1() throws Exception {
	
		System.out.println("\n\n-- TEST 1 --");
		
		/*
		 * A = a! (+) b!
		 * B = a?  +  b?  +  c?
		 * 
		 * A |x| B = true
		 */
		Contract A = internalSum("a", "b");
		Contract B = externalSum("a", "b", "c");
		assertTrue( ContractComplianceChecker.compliance(A, B) );
	}
	
	@Test
	public void test2() throws Exception {
		
		System.out.println("\n\n-- TEST 2 --");
	
		/*
		 * A = rec X. a! (+) b! (+) c!.X
		 * B = rec X. a?  +  b?  + c?.X
		 * 
		 * A |x| B = true
		 */		
		Recursion ra = recursion();
		Contract A = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction("c", ra)
				);
		ra.setContract(A);
		
		Recursion rb = recursion();
		Contract B = externalSum(
				externalAction("a"), 
				externalAction("b"), 
				externalAction("c", rb)
				);
		rb.setContract(B);
		assertTrue( ContractComplianceChecker.compliance(ra, rb) );
	}
	
	@Test
	public void test3() throws Exception {
		
		System.out.println("\n\n-- TEST 3 --");

		/*
		 * A = a! (+) b! (+) c!
		 * B = a?  +  b?
		 * 
		 * A |x| B = false
		 */		
		Contract A = internalSum("a", "b", "c");
		Contract B = externalSum("a", "b");
		assertFalse( ContractComplianceChecker.compliance(A, B) );
	}
	
	@Test
	public void test4() throws Exception {
		
		System.out.println("\n\n-- TEST 4 --");

		/*
		 * A = a! (+) b! . ( a?  +  c? . b! )
		 * B = a?  +  b? . ( a! (+) c! )
		 * 
		 * A |x| B = false
		 */		
		Contract A = internalSum(
				internalAction("a"),
				internalAction(
						"b",
						externalSum(
								externalAction("a"),
								externalAction(
										"c", internalSum("b")
								)
						)
				)
		);
		Contract B = externalSum(
				externalAction("a"),
				externalAction(
						"b",
						internalSum("a","c")
				)
		);
		assertFalse( ContractComplianceChecker.compliance(A, B) );
	}

}
