package it.unica.co2.api.contract;

public class InternalAction extends Action {

	private static final long serialVersionUID = 1L;
	
	public InternalAction(InternalAction a) {
		super(a);
	}
	
	public InternalAction(String name, Sort<?> sort, Contract next ) {
		super(name, sort, ActionType.INTERNAL, next);
	}

	@SuppressWarnings("unchecked")
	@Override
	public InternalAction next(Contract next) {
		this.next = next;
		next.setPreceeding(this);
		return this;
	}

}
