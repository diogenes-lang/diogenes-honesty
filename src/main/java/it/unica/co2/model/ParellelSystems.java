package it.unica.co2.model;

import java.util.ArrayList;
import java.util.List;

public class ParellelSystems extends CO2System {

	private List<CO2System> parallelSystems = new ArrayList<>();
	
	public boolean add(CO2System system) {
		return parallelSystems.add(system);
	}
	
	public boolean remove(CO2System system) {
		return parallelSystems.remove(system);
	}
}
