package it.unica.co2.api.process;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CO2Process implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	protected Logger logger;
	
	public CO2Process () {
		this.logger = LoggerFactory.getLogger(this.getClass().getName());
	}
	
	synchronized protected Thread parallel(Runnable process) {
//		logger.info("starting parallel process");
		Thread t = new Thread(process);
		t.start();
		return t;
	}
	
	
	protected void processCall(Class<? extends CO2Process> pClass, Object... args) {
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
			ctor = ConstructorUtils.getMatchingAccessibleConstructor(pClass, typesArray);	// get the constructor with the corresponding types
			
			if (ctor==null) {
				ctor = pClass.getDeclaredConstructor(typesArray);	// get the constructor with the corresponding types
			}

			assert ctor!=null;
			
			ctor.setAccessible(true);
			CO2Process p = ctor.newInstance(args);				// create a new instance passing the given args
			p.run();											// run the process
		}
		catch (	SecurityException | IllegalArgumentException | 
				InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException("error instantiating the class "+pClass, e);
		}
	}
	
}