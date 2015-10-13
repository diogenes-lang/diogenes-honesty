package it.unica.co2.api.contract.bekic;

import static it.unica.co2.api.contract.utils.ContractFactory.*;
import static org.junit.Assert.*;

import org.junit.Test;

import it.unica.co2.api.contract.Contract;
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
		
//		System.out.println(c.getContract().toTST());
//		System.out.println(cBekic.getContract().toTST());
		
		assertTrue(c!=cBekic);
		checkEnv(cBekic);
	}
	
	@Test
	public void test1() {
		
		System.out.println("+++++++++++ TEST 1 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		c1.setContract(internalSum().add("a1").add("b1", ref(c2)));
		c2.setContract(internalSum().add("a2").add("b2", ref(c1)));
		
		
		Bekic instance = Bekic.getInstance(c1,c2);
		
		System.out.println(c1);
		System.out.println(instance.defToRec(c1));
		
		System.out.println(c2);
		System.out.println(instance.defToRec(c2));
		
		checkEnv(instance.getEnv());
	}
	
	@Test
	public void test2() {
		
		System.out.println("+++++++++++ TEST 2 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		
		c1.setContract(internalSum().add("a", ref(c1)).add("b", ref(c2)));
		c2.setContract(internalSum().add("c", ref(c1)).add("d", ref(c2)));
		

		Bekic instance = Bekic.getInstance(c1,c2);
		
		System.out.println(c1);
		System.out.println(instance.defToRec(c1));
		
		System.out.println(c2);
		System.out.println(instance.defToRec(c2));
		
		checkEnv(instance.getEnv());
	}
	
	@Test
	public void test3() {
		
		System.out.println("+++++++++++ TEST 3 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		
		c1.setContract(internalSum().add("a").add("b", recursion("pippo").setContract(internalSum().add("pippo"))));
		c2.setContract(internalSum().add("c").add("d"));
		
		
		Bekic instance = Bekic.getInstance(c1,c2);
		
		System.out.println(c1);
		System.out.println(instance.defToRec(c1));
		
		System.out.println(c2);
		System.out.println(instance.defToRec(c2));
		
		checkEnv(instance.getEnv());
	}
	
	
	
	@Test
	public void test4() {
		System.out.println("+++++++++++ TEST 4 ++++++++++++");
		
		Recursion rec = recursion("x");
		rec.setContract(internalSum().add("a").add("b", recRef(rec)));
		
		Contract cBekic = Bekic.getInstance(rec).defToRec();
		
		System.out.println(rec);
		System.out.println(cBekic);
		
		assertTrue(rec!=cBekic);
		
	}
	
	
	@Test
	public void test5() {
		System.out.println("+++++++++++ TEST 5 ++++++++++++");
		
		Contract c = internalSum().add("a", empty());
		
		System.out.println(c);
		System.out.println(c.toMaude());
		System.out.println(c.toTST());
	}
	
	
	@Test
	public void test6() {
		System.out.println("+++++++++++ TEST 6 ++++++++++++");
		
		Recursion playerContract = recursion("x");
		
		Contract hit = internalSum().add("card", recRef(playerContract)).add("lose").add("abort");
		Contract end = internalSum().add("win").add("lose").add("abort");
		
		playerContract.setContract(externalSum().add("hit", hit).add("stand", end));
		
		System.out.println(playerContract);
		System.out.println(playerContract.toMaude());
		System.out.println(playerContract.toTST());
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



