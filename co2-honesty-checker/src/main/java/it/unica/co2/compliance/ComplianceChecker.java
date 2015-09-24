package it.unica.co2.compliance;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.lts.LTS;
import it.unica.co2.lts.LTSState;
import it.unica.co2.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComplianceChecker {

	
	public static boolean compliance(Contract a, Contract b) {
		
		System.out.println("================================================== COMPLIANCE CHECKER ");
		String aAsString = ObjectUtils.serializeObjectToStringQuietly(a);
		String bAsString = ObjectUtils.serializeObjectToStringQuietly(b);
		
		Config.enableLogging(false);
		Config conf = JPF.createConfig(new String[]{});
		
		try (
				InputStream jpfProps = HonestyChecker.class.getResourceAsStream("/jpf.properties");
				InputStream co2Props = HonestyChecker.class.getResourceAsStream("/co2.properties");
				)
		{
			conf.load(jpfProps);
			conf.load(co2Props);
		}
		catch (IOException e1) {
			throw new RuntimeException("unable to load the jpf config file", e1);
		}
		
		/*
		 * override jpf-core properties to point the embedded jars
		 */
		conf.setProperty("jpf-core.classpath", null);
		conf.setProperty("jpf-core.native_classpath", null);
		
		/*
		 * reorder the classpath entries so that co2apiHL-fake-<version>.jar appear first
		 */
		Stream<String> classpathEntries = Arrays.stream(System.getProperty("java.class.path").split(":"));
		
		String classpath = classpathEntries
			.sorted(
					(x,y) -> {
						if (x.contains("co2apiHL-fake")) {
			                return (y.contains("co2apiHL-fake")) ? 0 : -1;
			            } 
						else if (y.contains("co2apiHL-fake")) {
			                return 1;
			            } 
						else {
			                return 0;
			            }
					}
				)
			.collect(Collectors.joining(":"));
		
		// set the classpath
		System.out.println("using classpath: "+classpath);
		conf.append("classpath", classpath, ":");
		conf.append("native_classpath", classpath, ":");
		
		
		conf.setTarget(ComplianceChecker.class.getName());
		conf.setTargetEntry("jpfEntry([Ljava/lang/String;)V");
		conf.setTargetArgs(new String[]{aAsString, bAsString});
		
		if (!conf.getBoolean("compliance.print_SUT_output", false))
			conf.setProperty("vm.tree_output", "false");
		
		if (!conf.getBoolean("compliance.print_JPF_output", false)) {
			conf.setProperty("report.console.constraint", "constraint,snapshot");
			conf.remove("report.console.finished");
			conf.remove("report.console.probe");
			conf.remove("report.console.property_violation"); 
			conf.remove("report.console.start"); 
			conf.remove("report.console.transition"); 
		}
		
		if (conf.getBoolean("compliance.print_JPF_properties", false))
			conf.printEntries();

		JPF jpf = new JPF(conf);
		
		ComplianceListener complianceListener = new ComplianceListener();
		jpf.addListener(complianceListener);
		
		try {
			System.out.println("contract a: "+a);
			System.out.println("contract b: "+b);
			System.out.println("starting JPF to checking compliance...");
			System.out.println("--------------------------------------------------");
			jpf.run();
			
			boolean compliance;
			
			if (jpf.foundErrors()){
				// ... process property violations discovered by JPF
				System.out.println("JPF found an error");
				
				gov.nasa.jpf.Error error = jpf.getLastError();
				
				System.out.println("error details: "+error.getDetails());
				
				if (error.getProperty() instanceof ComplianceListener) {
					ComplianceListener property = (ComplianceListener) error.getProperty();
					
					if (
							property.getPath()!=null && property.getPath().size()>1 &&
									conf.getBoolean("compliance.print_path_on_fail", false)
							) {
						System.out.println("--------------------------------------------------");
						printPath(property.getPath());
					}
					
					if (property.getFinalState()!=null) {
						System.out.println("--------------------------------------------------");
						System.out.println("the contract-configuration not safe is \n"+property.getFinalState());
					}
				}
				
				compliance = false;
			}
			else {
				System.out.println("JPF ends without errors");
				compliance = true;
			}
			
			System.out.println("--------------------------------------------------");
			System.out.println("contract a: "+a);
			System.out.println("contract b: "+b);
			System.out.println("compliance: "+compliance);
			System.out.println("==================================================");
			return compliance;
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
		
		System.out.println("JPF - contract a (serialized): "+aAsString);
		System.out.println("JPF - contract b (serialized): "+bAsString);
		
		Contract a = ObjectUtils.deserializeObjectFromString(aAsString, Contract.class);
		Contract b = ObjectUtils.deserializeObjectFromString(bAsString, Contract.class);
		
		System.out.println("JPF - contract a: "+a);
		System.out.println("JPF - contract b: "+b);
		
		LTS lts = new LTS( new ContractConfiguration(a, b));
		lts.start();
	}
	
	private static String printPath(List<LTSState> path) {
		
		for (LTSState s : path) {
			if (s.getPrecededTransition()!=null) {
				System.out.println("    |   ");
				System.out.println("    |   ");
				System.out.println("    "+s.getPrecededTransition());
				System.out.println("    |  ");
				System.out.println("    V   ");
			}
			System.out.println(s);
		}
		
		return null;
	}
}
