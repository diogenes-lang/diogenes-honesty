package it.unica.co2.test;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.empty;
import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;
import static it.unica.co2.api.contract.utils.ContractFactory.ref;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.contract.bekic.Bekic;
import it.unica.co2.api.contract.utils.ContractExplorer;



public class BekicTest {

	@Test
	public void test() {
		System.out.println("+++++++++++ TEST   ++++++++++++");
		
		// C = a! (+) b! . C    <=>   C = rec X . ( a! (+) b! . X )
		
		ContractDefinition c = def("c");
		c.setContract(internalSum().add("a").add("b", ref(c)));
		
		ContractDefinition cBekic = Bekic.getInstance(c).defToRec(c);
		
		System.out.println(c);
		System.out.println(cBekic);
		
		System.out.println(c.getContract().toTST());
		System.out.println(cBekic.getContract().toTST());
		
		assertTrue(c!=cBekic);
		checkEnv(cBekic);
	}
	
	@Test
	public void test1() {
		
		System.out.println("+++++++++++ TEST 1 ++++++++++++");
		
		/*
		 * c1 = a1! (+) b1! . c2
		 * c2 = a2! (+) b2! . c1
		 */		
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
		System.out.println(instance.defToRec(c1).getContract().toMaude());
		
		System.out.println(c2);
		System.out.println(instance.defToRec(c2));
		System.out.println(instance.defToRec(c2).getContract().toMaude());
		
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
		
		SessionType cBekic = Bekic.getInstance(rec).defToRec();
		
		System.out.println(rec);
		System.out.println(cBekic);
		
		assertTrue(rec!=cBekic);
		
	}
	
	
	@Test
	public void test5() {
		System.out.println("+++++++++++ TEST 5 ++++++++++++");
		
		SessionType c = internalSum().add("a", empty());
		
		System.out.println(c);
		System.out.println(c.toMaude());
		System.out.println(c.toTST());
	}
	
	
	@Test
	public void test6() {
		System.out.println("+++++++++++ TEST 6 ++++++++++++");
		
		Recursion playerContract = recursion("x");
		
		SessionType hit = internalSum().add("card", recRef(playerContract)).add("lose").add("abort");
		SessionType end = internalSum().add("win").add("lose").add("abort");
		
		playerContract.setContract(externalSum().add("hit", hit).add("stand", end));
		
		System.out.println(playerContract);
		System.out.println(playerContract.toMaude());
		System.out.println(playerContract.toTST());
	}
	
	@Test
	public void test7() {
		System.out.println("+++++++++++ TEST 7 ++++++++++++");
		
		ContractDefinition C = def("C");
		ContractDefinition Cread = def("Cread");
		ContractDefinition Cwrite = def("Cwrite");
		
		C.setContract(externalSum().add("req", Sort.string(), internalSum().add("ackR", ref(Cread)).add("ackW", ref(Cwrite)).add("error")));
		Cread.setContract(internalSum().add("data", Sort.integer(), externalSum().add("ack", Sort.integer(), ref(Cread))).add("error"));
		Cwrite.setContract(empty());
		
		System.out.println(C.getContract());
		System.out.println(C.getContract().toMaude());
		System.out.println(C.getContract().toTST());
	}
	
	@Test
	public void test8() {
		System.out.println("+++++++++++ TEST 8 ++++++++++++");
		
		ContractDefinition A = def("A").setContract(
				externalSum()
				.add("quote", Sort.integer(), 
						internalSum()
						.add("pay", 
								externalSum()
								.add("confirm", 
										internalSum()
										.add("commit")
										.add("abort")))
						.add("abort")));;
		ContractDefinition B = def("B").setContract(internalSum().add("b", ref(A)));
		ContractDefinition C = def("C").setContract(internalSum().add("c", ref(A)));

		System.out.println(A.getContract().toTST());
		System.out.println(B.getContract().toTST());
		System.out.println(C.getContract().toTST());
		System.out.println(A);
		System.out.println(B);
		System.out.println(C);
		System.out.println(A.getContract().toMaude());
		System.out.println(B.getContract().toMaude());
		System.out.println(C.getContract().toMaude());
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



