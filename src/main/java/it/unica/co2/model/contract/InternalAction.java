package it.unica.co2.model.contract;

public class InternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public InternalAction(String name) {
		super(name, Sort.UNIT, Type.EXTERNAL);
	}
	
	public InternalAction(String name, Sort sort) {
		super(name, sort, Type.INTERNAL);
	}

	public InternalAction(String name, Sort sort, Contract next ) {
		super(name, sort, Type.INTERNAL, next);
	}

}
