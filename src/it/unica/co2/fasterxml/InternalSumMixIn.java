package it.unica.co2.fasterxml;

import it.unica.co2.model.contract.InternalAction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class InternalSumMixIn extends ContractMixIn {

	@JsonCreator
	public InternalSumMixIn(@JsonProperty("actions")InternalAction... actions) {}
	
}
