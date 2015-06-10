package it.unica.co2.model;

import java.util.HashSet;
import java.util.Set;

public abstract class CO2System {

	private Set<String> names = new HashSet<>();

	public boolean bind(String x) {
		return names.add(x);
	}

	public boolean unbind(String x) {
		return names.remove(x);
	}
}
