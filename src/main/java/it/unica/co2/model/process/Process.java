package it.unica.co2.model.process;

import it.unica.co2.util.Logger;


public abstract class Process implements Runnable {

	protected static Logger logger;
	
	protected Process(String username) {
		logger = Logger.getInstance(username, System.out, this.getClass().getSimpleName());
	}

}
