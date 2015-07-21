package it.unica.co2.model.contract;

public class InternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public InternalAction(String name, Sort sort, Contract next ) {
		super(name, sort, Type.INTERNAL, next);
	}

	@SuppressWarnings("unchecked")
	@Override
	public InternalAction next(Contract next) {
		return next(next, InternalAction.class);
	}

}
