package it.unica.co2.api.contract.bekic;

import static it.unica.co2.api.contract.utils.ContractFactory.*;
import static org.junit.Assert.*;

import org.junit.Test;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.utils.ContractExplorer;



public class BekicTest {

	@Test
	public void test() {
		System.out.println("+++++++++++ TEST   ++++++++++++");
		
		// C = a (+) b . C
		
		ContractDefinition c = def("c");
		c.setContract(internalSum().add("a").add("b", ref(c)));
		
		// C = rec X . ( a (+) b . X )
		
		ContractDefinition cBekic = Bekic.getInstance(c).defToRec(c);
		
		System.out.println(c);
		System.out.println(cBekic);
		
		checkEnv(cBekic);
	}
	
	@Test
	public void test1() {
		
		System.out.println("+++++++++++ TEST 1 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		c1.setContract(internalSum().add("a1").add("b1", ref(c2)));
		c2.setContract(internalSum().add("a2").add("b2", ref(c1)));
		
		ContractDefinition[] env = Bekic.getInstance(c1,c2).defToRec();
		
		
		System.out.println(c1);
		System.out.println(c2);
		
		System.out.println(env[0]);
		System.out.println(env[1]);
		
		checkEnv(env);
	}
	
	@Test
	public void test2() {
		
		System.out.println("+++++++++++ TEST 2 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		
		c1.setContract(internalSum().add("a", ref(c1)).add("b", ref(c2)));
		c2.setContract(internalSum().add("c", ref(c1)).add("d", ref(c2)));
		
		ContractDefinition[] env = Bekic.getInstance(c1,c2).defToRec();
		
		checkEnv(env);
	}
	
	@Test
	public void test3() {
		
		System.out.println("+++++++++++ TEST 3 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		
		c1.setContract(internalSum().add("a").add("b", recursion("pippo").setContract(internalSum().add("pippo"))));
		c2.setContract(internalSum().add("c").add("d"));
		
		ContractDefinition[] env = Bekic.getInstance(c1,c2).defToRec();
		
		checkEnv(env);
	}
	
	
	@Test
	public void testNoSideEffect() {
		
		System.out.println("+++++++++++ TEST 4 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		
		c1.setContract(internalSum().add("a", ref(c1)).add("b", ref(c2)));
		c2.setContract(internalSum().add("c", ref(c1)).add("d", ref(c2)));
		
		Bekic bekic = Bekic.getInstance(c1, c2);
		bekic.defToRec();
		
		assertTrue(c1!=bekic.defToRec(c1));
		assertTrue(c2!=bekic.defToRec(c2));
	}
	
	
	private void checkEnv(ContractDefinition...  cDefs) {
		
		for (ContractDefinition c : cDefs) {

			// contractReference-free
			ContractExplorer.findAll(
					c.getContract(), 
					ContractReference.class,
					(x)-> {
						fail();
					}
				);
			
			
			// each recRef points to a Recursion in the same contract before reaching the reference itself
			BooleanW flag = new BooleanW();
			flag.value = false;
			
			ContractExplorer.findAll(
					c.getContract(), 
					RecursionReference.class,
					(x)-> {
						ContractExplorer.findAll(
								c.getContract(),
								Recursion.class,
								(y)-> (x.getReference()==y),
								(y)-> {
									flag.value=true;
								}
							);
						
						assertTrue(flag.value);
						flag.value = false;
					}
				);
			
			// each recRef points to a Recursion in the same contract before reaching the reference itself
			flag.value = false;
			
			ContractExplorer.findAll(
					c.getContract(), 
					Recursion.class,
					(x)-> {
						ContractExplorer.findAll(
								c.getContract(),
								RecursionReference.class,
								(y)-> (y.getReference()==x),
								(y)-> {
									flag.value=true;
								}
							);
						
						assertTrue(flag.value);
						flag.value = false;
					}
				);
						
		}
	}
	

	private static class BooleanW {
		boolean value;
	}
}



