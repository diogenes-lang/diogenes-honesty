package it.unica.co2.api;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.util.ObjectUtils;
import co2api.ContractException;
import co2api.TST;


public class TST2 extends TST {

	@SuppressWarnings("unused")
	private String serializedContract;
	
	public TST2(Contract c) throws ContractException {
		super(c.toMiddleware());
		this.serializedContract = ObjectUtils.serializeObjectToStringQuietly(c);
	}
}
