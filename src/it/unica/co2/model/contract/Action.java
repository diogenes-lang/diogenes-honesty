package it.unica.co2.model.contract;

import java.io.Serializable;

public abstract class Action implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Type { 
		INTERNAL,
		EXTERNAL
	}
	
	public enum Sort { 
		UNIT, 
		INT,
		STRING
	}
	
	private final String name;
	private final Sort sort;
	private final Contract next;
	private final Type type;
	
	public Action(String name, Sort sort, Type type) {
		this(name, sort, type, null);
	}
	
	public Action(String name, Sort sort, Type type, Contract next) {
		this.name = name;
		this.sort = sort;
		this.type = type;
		this.next = next;
	}

	public String getName() {
		return name;
	}

	public Sort getSort() {
		return sort;
	}

	public Type getType() {
		return type;
	}

	public Contract getNext() {
		return next;
	}

	@Override
	public String toString() {
		return name + (this instanceof InternalAction?"!":"?") + (sort!=Sort.UNIT? " :"+sort: "") + (next!=null? "."+next.toString(): "") ;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sort == null) ? 0 : sort.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Action other = (Action) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sort != other.sort)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
