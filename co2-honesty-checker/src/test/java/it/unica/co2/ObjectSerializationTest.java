package it.unica.co2;

import static it.unica.co2.api.contract.utils.ContractFactory.*;
import static org.junit.Assert.*;

import org.junit.Test;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.util.ObjectUtils;


public class ObjectSerializationTest {

	@Test
	public void test() {

		ContractDefinition Cp = def("Cp");
		ContractDefinition Cd = def("Cd");
		
		Cp.setContract(externalSum().add("hit", Sort.UNIT, internalSum().add("card", Sort.INT).add("lose", Sort.UNIT).add("abort", Sort.UNIT)).add("stand", Sort.UNIT, internalSum().add("win", Sort.UNIT).add("lose", Sort.UNIT).add("abort", Sort.UNIT)));
		Cd.setContract(internalSum().add("next", Sort.UNIT, externalSum().add("card", Sort.INT)).add("abort", Sort.UNIT));
		
		ObjectUtils.serializeObjectToStringQuietly(Cp.getContract());
		ObjectUtils.serializeObjectToStringQuietly(Cd.getContract());
	}

}
