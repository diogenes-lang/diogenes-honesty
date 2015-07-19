package it.unica.co2.model.process;

import java.io.Serializable;

import it.unica.co2.util.Logger;


public abstract class CO2Process implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	
	protected Logger logger;
	
	protected CO2Process(String loggerName) {
		logger = Logger.getInstance(loggerName, System.out, this.getClass().getSimpleName());
	}

}
