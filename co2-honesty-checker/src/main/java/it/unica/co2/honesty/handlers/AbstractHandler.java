package it.unica.co2.honesty.handlers;

import java.util.logging.Logger;

import gov.nasa.jpf.JPF;

abstract class AbstractHandler implements IHandler {

	protected final Logger log;
	
	protected AbstractHandler() {
		log = JPF.getLogger(this.getClass().getName());
	}
	
}
