package it.unica.co2.fasterxml;

import it.unica.co2.model.contract.Action.Sort;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ExternalActionMixIn {

	@JsonCreator
	public ExternalActionMixIn(
			@JsonProperty("name") String name, 
			@JsonProperty("sort") Sort sort, 
			@JsonProperty("next") ContractMixIn next
			) {}

}
