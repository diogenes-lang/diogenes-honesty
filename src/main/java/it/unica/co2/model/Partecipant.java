package it.unica.co2.model;

import it.unica.co2.model.process.Process;

public class Partecipant extends CO2System {
	
	private final String name;
	private final Process process;
	
	public Partecipant(String name, Process process) {
		this.name = name;
		this.process = process;
	}

	public String getName() {
		return name;
	}

	public Process getProcess() {
		return process;
	}

}
