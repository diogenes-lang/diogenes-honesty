package it.unica.co2.semantics;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import it.unica.co2.model.contract.Contract;

public class ModelChecker {

	private static final JPF jpf;
	
	static {
		Config conf = JPF.createConfig(new String[]{"-log", "+site=/home/nicola/xtext_projects/jpf/site.properties", "+shell.port=4242"});
		conf.setTarget(ModelChecker.class.getName());
		
		conf.setProperty("cg.enumerate_random", "true");
		conf.setTarget("it.unica.co2.semantics.ModelChecker");
		conf.setTargetEntry("jpfEntry()V");
		
		conf.append("native_classpath", "${jpf-core}/build/jpf.jar", ";");
		conf.append("native_classpath", "./bin/", ";");
		conf.append("native_classpath", "./lib/commons-lang-2.6.jar", ";");
		
		conf.append("classpath", "${jpf-core}/build/jpf.jar", ";");
		conf.append("classpath", "./bin/", ";");
		conf.append("classpath", "./lib/commons-lang-2.6.jar", ";");
		
		conf.append("sourcepath", "./src/", ";");
		
		jpf = new JPF(conf);
		
	}
	
	
	
	
	private static Contract a;
	private static Contract b;
	
	public static boolean compliant(Contract a, Contract b) {
		
		ModelChecker.a = a;
		ModelChecker.b = b;
		
		System.out.println("starting JPF for checking compliance");
		
		try {
			jpf.run();
		}
		catch (JPFConfigException e){
			// ... handle configuration exception
			// ...  can happen before running JPF and indicates inconsistent configuration data
			e.printStackTrace();
			return false;
		}
		catch (JPFException e){
			// ... handle exception while executing JPF, can be further differentiated into
			// ...  JPFListenerException - occurred from within configured listener
			// ...  JPFNativePeerException - occurred from within MJI method/native peer
			// ...  all others indicate JPF internal errors
			e.printStackTrace();
			return false;
		}

		
		
		if (jpf.foundErrors()){
			// ... process property violations discovered by JPF
			System.out.println("JPF found an error");
			System.out.println("error details: "+jpf.getLastError().getDetails());
			return false;
		}
		else {
			System.out.println("JPF ends without errors");
			return true;
		}
		
	}
	
	
	
	public static void jpfEntry() {

		LTS lts = new LTS( new ContractConfiguration(a, b));
		lts.start();
	}
}
