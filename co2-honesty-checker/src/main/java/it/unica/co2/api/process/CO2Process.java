package it.unica.co2.api.process;

import it.unica.co2.util.Logger;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


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
	
	
	public void processCall(Class<? extends CO2Process> pClass, Object... args) {
		this.processCall(pClass, pClass.getSimpleName(), args);
	}
	
	private void processCall(Class<? extends CO2Process> pClass, String processName, Object... args) {
		
//		System.out.println(pClass.getDeclaredConstructors()[0].toString());
		
		// get all arguments types
		List<Class<?>> types = new ArrayList<>();
		for (Object arg : args) {
//			System.out.println(arg.getClass());
			types.add(arg.getClass());
		}
		
		// convert to array
		Class<?>[] typesArray = types.toArray(new Class<?>[]{});
		
		Constructor<? extends CO2Process> ctor;
		try {
			ctor = pClass.getDeclaredConstructor(typesArray);	// get the constructor with the corresponding types
			ctor.setAccessible(true);
			CO2Process p = ctor.newInstance(args);				// create a new instance passing the given args
			p.run();											// run the process
		}
		catch (	SecurityException | IllegalArgumentException | NoSuchMethodException | 
				InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("error instantiating the class "+pClass, e);
		}
	}
}