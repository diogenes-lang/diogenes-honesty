package it.unica.co2.model;

import java.util.HashMap;
import java.util.Map;

import co2api.Session;

public abstract class Process implements Runnable {

	private Map<String, Session<?>> sessions = new HashMap<>();

	public Map<String, Session<?>> getSessions() {
		return sessions;
	}

	public void setSessions(Map<String, Session<?>> sessions) {
		this.sessions = sessions;
	}
	
}
