package it.unica.co2.semantics;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.util.ObjectUtils;

public class ContractComplianceChecker {

	
	public static boolean compliance(Contract a, Contract b) throws Exception {

		
		
		String aAsString = ObjectUtils.serializeObjectToString(a);
		String bAsString = ObjectUtils.serializeObjectToString(b);
		
		Config.enableLogging(false);
		Config conf = JPF.createConfig(
				new String[]{"-log", "+site=/home/nicola/xtext_projects/jpf/site.properties", "+shell.port=4242"}
		);
		
		conf.setTarget(ContractComplianceChecker.class.getName());
		conf.setTargetEntry("jpfEntry([Ljava/lang/String;)V");
		conf.setTargetArgs(new String[]{aAsString, bAsString});
		
		JPF jpf = new JPF(conf);
		
		try {
			
			System.out.println("starting JPF for checking compliance");
			System.out.println("Contract a: "+a);
			System.out.println("Contract b: "+b);
			jpf.run();
			
			if (jpf.foundErrors()){
				// ... process property violations discovered by JPF
				System.out.println("JPF found an error");
				
				gov.nasa.jpf.Error error = jpf.getLastError();
				
				System.out.println("error details: "+error.getDetails());
				
				System.out.println(error.getPath().getClass());
				System.out.println(error.getProperty());
				
				return false;
			}
			else {
				System.out.println("JPF ends without errors");
				System.out.println("contract a: "+a);
				System.out.println("contract b: "+b);
				System.out.println("compliance: "+true);
				return true;
			}
		}
		catch (JPFConfigException e){
			// ... handle configuration exception
			// ...  can happen before running JPF and indicates inconsistent configuration data
			e.printStackTrace();
		}
		catch (JPFException e){
			// ... handle exception while executing JPF, can be further differentiated into
			// ...  JPFListenerException - occurred from within configured listener
			// ...  JPFNativePeerException - occurred from within MJI method/native peer
			// ...  all others indicate JPF internal errors
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	public static void jpfEntry(String[] args) throws Exception {
		
		assert args.length==2;
		
		String aAsString = args[0];
		String bAsString = args[1];
		
		System.out.println("JPF - JSON a: "+aAsString);
		System.out.println("JPF - JSON b: "+bAsString);
		
		Contract a = ObjectUtils.deserializeObjectFromString(aAsString, Contract.class);
		Contract b = ObjectUtils.deserializeObjectFromString(bAsString, Contract.class);
		
		System.out.println("JPF - contract a: "+a);
		System.out.println("JPF - contract b: "+b);
		
		LTS lts = new LTS( new ContractConfiguration(a, b));
		lts.start();
	}
}
