package it.unica.co2.model.process;

import java.io.Serializable;

import it.unica.co2.util.Logger;


public abstract class Process implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	
	protected static Logger logger;
	
	protected Process(String username) {
		logger = Logger.getInstance(username, System.out, this.getClass().getSimpleName());
	}

}
