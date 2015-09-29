package it.unica.co2.api.contract.defeq;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import static org.junit.Assert.*;

import java.util.List;

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
		
		ContractExplorer expl = new ContractExplorer();
		
		System.out.println(c);
		
		List<ContractReference> contracts = expl.findall(c, ContractReference.class, (x)-> {return true;}, (x)->{x.getPreceeding().next(internalSum().add("pippo"));});
	
		assertEquals(3, contracts.size());
		
		System.out.println(c);
	}

}
