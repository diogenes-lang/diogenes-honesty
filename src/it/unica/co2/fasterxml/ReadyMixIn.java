package it.unica.co2.fasterxml;

import it.unica.co2.model.contract.ExternalAction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ReadyMixIn extends ContractMixIn {

	@JsonCreator
	public ReadyMixIn(@JsonProperty("action") ExternalAction action) {}
	
}
