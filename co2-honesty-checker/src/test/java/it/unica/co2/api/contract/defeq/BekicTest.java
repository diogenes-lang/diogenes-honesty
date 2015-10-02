package it.unica.co2.api.contract.defeq;

import static it.unica.co2.api.contract.utils.ContractFactory.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.bekic.Bekic;
import it.unica.co2.api.contract.utils.ContractExplorer;



public class BekicTest {

	@Test
	public void test() {
		System.out.println("+++++++++++ TEST   ++++++++++++");
		ContractDefinition c1 = def("c1");
		c1.setContract(internalSum().add("a").add("b", ref(c1)));
		
		ContractDefinition[] env = Bekic.getInstance(c1).defToRec();
		
		checkEnv(env);
	}
	
	@Test
	public void test1() {
		
		System.out.println("+++++++++++ TEST 1 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		c1.setContract(internalSum().add("a1").add("b1", ref(c2)));
		c2.setContract(internalSum().add("a2").add("b2", ref(c1)));
		
		ContractDefinition[] env = Bekic.getInstance(c1,c2).defToRec();
		
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
			
			List<ContractReference> crefs = new ArrayList<>();
			ContractExplorer.findAll(c.getContract(), ContractReference.class, (x)-> {crefs.add(x);});
			
			assertTrue( crefs.isEmpty() );
		}
	}
	
	
}
