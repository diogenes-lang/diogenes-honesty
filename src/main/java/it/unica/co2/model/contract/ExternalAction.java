package it.unica.co2.model.contract;

public class ExternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public ExternalAction(String name) {
		super(name, Sort.UNIT, Type.EXTERNAL);
	}

	public ExternalAction(String name, Sort sort) {
		super(name, sort, Type.EXTERNAL);
	}

	public ExternalAction( String name, Sort sort, Contract next) {
		super(name, sort, Type.EXTERNAL, next);
	}

}
