package it.unica.co2.model.contract;



public class ExternalAction extends Action {

	public ExternalAction(String name, Sort sort) {
		super(name, sort);
	}

	public ExternalAction(String name, Sort sort, Contract next) {
		super(name, sort, next);
	}

}
