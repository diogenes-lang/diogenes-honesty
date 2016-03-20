package it.unica.co2.honesty.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nasa.jpf.JPF;

public abstract class AbstractHandler<T> implements HandlerI<T> {

	public static Level level = Level.OFF;
	protected final Logger log;

	public AbstractHandler() {
		log = JPF.getLogger(this.getClass().getName());
		log.setLevel(level);;
	}

}