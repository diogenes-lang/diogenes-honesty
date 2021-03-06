package it.unica.co2.api.contract;

public class EmptyContract extends Sum<Action> {

	private static final long serialVersionUID = 1L;

	@Override
	public EmptyContract add(String name, Sort<?> sort, String guard, SessionType next) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return "0";
	}

	@Override
	public SessionType deepCopy() {
		return new EmptyContract();
	}
}
