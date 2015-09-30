package it.unica.co2.api.contract.defeq;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.contract.newapi.ContractDefinition;
import it.unica.co2.api.contract.newapi.ContractReference;



public class ContractExplorerTest {

	@Test
	public void test() {
		
		ContractDefinition w1 = def("w1").setContract(internalSum().add("1"));
		ContractDefinition w2 = def("w2").setContract(internalSum().add("2"));
		ContractDefinition w3 = def("w3").setContract(internalSum().add("3"));
		
		
		Contract c = internalSum()
				.add("a", externalSum().add("a1", ref(w1)))
				.add("b", ref(w2))
				.add("c")
				.add("d", ref(w3));
		
		System.out.println(c);
		
		List<ContractReference> contracts = ContractExplorer.findall(c, ContractReference.class, (x)-> {return true;}, (x)->{x.getPreceeding().next(internalSum().add("pippo"));});
	
		assertEquals(3, contracts.size());
		
		System.out.println(c);
	}

	
	
	@Test
	public void testGetAllRefs() {
		
		System.out.println("+++++++++++ TEST 4 ++++++++++++");
		
		ContractDefinition c1 = def("c1");
		ContractDefinition c2 = def("c2");
		ContractDefinition c3 = def("c2");
		ContractDefinition c4 = def("c2");
		
		c1.setContract(internalSum().add("a", ref(c1)).add("b", ref(c2)));
		c2.setContract(internalSum().add("c", ref(c1)).add("d", ref(c3)));
		c3.setContract(internalSum().add("a", ref(c4)).add("b"));
		c4.setContract(internalSum().add("c", ref(c4)).add("d", ref(c3)));
		
		Set<ContractDefinition> refs = ContractExplorer.getAllReferences(c1);
		
		assertEquals(4, refs.size());
		assertTrue(refs.contains(c1));
		assertTrue(refs.contains(c2));
		assertTrue(refs.contains(c3));
		assertTrue(refs.contains(c4));

		refs = ContractExplorer.getAllReferences(c4);
		
		assertEquals(2, refs.size());
		assertTrue(refs.contains(c4));
		assertTrue(refs.contains(c3));
		assertFalse(refs.contains(c1));
		assertFalse(refs.contains(c2));
		
	}
}
