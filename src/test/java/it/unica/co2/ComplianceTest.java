package it.unica.co2;

import static it.unica.co2.model.CO2Factory.*;
import static org.junit.Assert.*;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.semantics.ContractComplianceChecker;

import org.junit.Test;

public class ComplianceTest {

	@Test
	public void compliance() throws Exception {
		
		Contract A;
		Contract B;
		
		/*
		 * A = a! (+) b!
		 * B = a?  +  b?  +  c?
		 * 
		 * A |x| B = true
		 */
		A = internalSum("a", "b");
		B = externalSum("a", "b", "c");
		assertTrue( ContractComplianceChecker.compliance(A, B) );
		
		/*
		 * A = rec X. a! (+) b! (+) c!.X
		 * B = rec X. a?  +  b?  + c?.X
		 * 
		 * A |x| B = true
		 */		
		Recursion ra = recursion();
		A = internalSum(
				internalAction("a"),
				internalAction("b"),
				internalAction("c", ra)
		);
		ra.setContract(A);
		
		Recursion rb = recursion();
		B = externalSum(
				externalAction("a"), 
				externalAction("b"), 
				externalAction("c", rb)
		);
		rb.setContract(B);
		assertTrue( ContractComplianceChecker.compliance(ra, rb) );
	}

}
