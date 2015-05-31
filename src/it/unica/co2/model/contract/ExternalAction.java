package it.unica.co2.model.contract;




public class ExternalAction extends Action {

	public ExternalAction(String name, Sort sort) {
		super(name, sort, Type.EXTERNAL);
	}

	public ExternalAction(String name, Sort sort, Contract next) {
		super(name, sort, Type.EXTERNAL, next);
	}

}
