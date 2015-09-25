package it.unica.co2.api.contract;


public class ExternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public ExternalAction( String name, Sort sort, Contract next) {
		super(name, sort, ActionType.EXTERNAL, next);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ExternalAction next(Contract next) {
		return next(next, ExternalAction.class);
	}
}
