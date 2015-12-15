package it.unica.co2.api.contract;


public class ExternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public ExternalAction(ExternalAction a) {
		super(a);
	}
	
	public ExternalAction( String name, Sort<?> sort, String guard, Contract next) {
		super(name, sort, ActionType.EXTERNAL, guard, next);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ExternalAction next(Contract next) {
		this.next=next;
		next.setPreceeding(this);
		return this;
	}
}
