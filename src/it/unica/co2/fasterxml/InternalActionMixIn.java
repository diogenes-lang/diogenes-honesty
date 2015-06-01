package it.unica.co2.fasterxml;

import it.unica.co2.model.contract.Action.Sort;
import it.unica.co2.model.contract.Contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class InternalActionMixIn {

	@JsonCreator
	public InternalActionMixIn(
			@JsonProperty("name") String name, 
			@JsonProperty("sort") Sort sort, 
			@JsonProperty("next") Contract next
			) {}

}
