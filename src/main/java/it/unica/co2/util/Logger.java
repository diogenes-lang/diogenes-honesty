package it.unica.co2.util;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Logger {

	private static final Map<String, Logger> instances = new HashMap<>();
	
	private final PrintStream out;
	private final String logName;
	
	private Logger(String username, PrintStream out, String logname) {
		this.out=out;
		this.username = username;
		this.logName = logname;
	}
	
	private String username = "";

	public void log() {
		log("");
	}
	
	public void log(Object str) {
		if (username!=null && !username.isEmpty())
			out.println("["+username+"] "+logName+" - "+str.toString());
		else
			out.println(str);
	}
	
	
	public static Logger getInstance(String username, PrintStream out, String logname) {
		
		Logger logger = instances.containsKey(logname)? 
				instances.get(logname): 
				new Logger(username, out, logname);
		
		instances.put(logname, logger);
		
		return new Logger(username, out, logname);
	}

	public static Logger getInstance(String username, PrintStream out, Class<?> clazz) {
		return getInstance(username, out, clazz.getSimpleName());
	}
}
