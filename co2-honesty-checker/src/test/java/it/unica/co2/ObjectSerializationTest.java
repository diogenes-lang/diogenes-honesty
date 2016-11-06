package it.unica.co2;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import org.junit.Test;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.util.ObjectUtils;


public class ObjectSerializationTest {

	@Test
	public void test() {

		ContractDefinition Cp = def("Cp");
		ContractDefinition Cd = def("Cd");
		
		Cp.setContract(externalSum().add("hit", internalSum().add("card", Sort.integer()).add("lose").add("abort")).add("stand", internalSum().add("win").add("lose").add("abort")));
		Cd.setContract(internalSum().add("next", externalSum().add("card", Sort.integer())).add("abort"));
		
		ObjectUtils.serializeObjectToStringQuietly(Cp.getContract());
		ObjectUtils.serializeObjectToStringQuietly(Cd.getContract());
	}

}
