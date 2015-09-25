package it.unica.co2.api.process;

import it.unica.co2.util.Logger;

import java.io.Serializable;


public abstract class CO2Process implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	protected transient Logger logger;
	
	protected CO2Process(String loggerName) {
		logger = Logger.getInstance(loggerName, System.out, this.getClass().getSimpleName());
	}
	
	synchronized public long parallel(Runnable process) {
		logger.log("starting parallel process");
		Thread t = new Thread(process);
		t.start();
		return t.getId();
	}
}