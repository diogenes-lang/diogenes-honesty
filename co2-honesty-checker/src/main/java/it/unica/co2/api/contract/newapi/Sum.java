package it.unica.co2.api.contract.newapi;

public abstract class Sum<T extends Action> extends Contract {

	private static final long serialVersionUID = 1L;

	protected final T[] actions;
	private final Class<T> clazz;
	
	@SafeVarargs
	protected Sum(Class<T> clazz, T... actions) {
		this.clazz = clazz;
		this.actions=actions;
	}
	
	public T[] getActions() {
		return actions;
	}
	
	public Sum<T> add(String action) {
		return add(action, Sort.UNIT);
	}

	public Sum<T> add(String action, Sort sort) {
		return add(action, sort, null);
	}
	
	public Sum<T> add(String action, Contract next) {
		return add(action, Sort.UNIT, next);
	}
	
	abstract public Sum<T> add(String action, Sort sort, Contract next);
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actions == null) ? 0 : actions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sum<?> other = (Sum<?>) obj;
		if (actions == null) {
			if (other.actions != null)
				return false;
		}
		else if (!actions.equals(other.actions))
			return false;
		return true;
	}
	
}
