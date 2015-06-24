package it.unica.co2.model.prefix;

import it.unica.co2.util.Logger;


public abstract class Prefix implements Runnable {

	protected static Logger logger;
	
	protected Prefix(String username) {
		logger = Logger.getInstance(username, System.out, this.getClass().getSimpleName());
	}
	
}
