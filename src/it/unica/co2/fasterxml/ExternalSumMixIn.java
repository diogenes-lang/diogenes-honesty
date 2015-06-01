package it.unica.co2.fasterxml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ExternalSumMixIn extends ContractMixIn {

	@JsonCreator
	public ExternalSumMixIn(@JsonProperty("actions")ExternalActionMixIn... actions) {}
	
}
