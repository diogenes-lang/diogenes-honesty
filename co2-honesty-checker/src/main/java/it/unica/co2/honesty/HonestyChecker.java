package it.unica.co2.honesty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.Statistics.Event;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.util.ObjectUtils;

public class HonestyChecker {
	
	public static Statistics stats;
	
	synchronized public static <T extends Participant> HonestyResult isHonest(Class<T> participantClass, Object... args) {
		
		stats = new Statistics();
		stats.update(Event.HONESTY_START);
		
		System.out.println("================================================== HONESTY CHECKER ");
		System.out.println("checking the honesty of "+participantClass.getName());
		
		
		// get all arguments types
		List<Class<?>> types = new ArrayList<>();
		for (Object arg : args) {
			types.add(arg.getClass());
		}
		
		// convert to array
		Class<?>[] typesArray = types.toArray(new Class<?>[]{});
		
		Participant participant;
		Constructor<? extends Participant> ctor;

		try {
			ctor = participantClass.getDeclaredConstructor(typesArray);	// get the constructor with the corresponding types
			ctor.setAccessible(true);
			participant = ctor.newInstance(args);				// create a new instance passing the given args
		}
		catch (	SecurityException | IllegalArgumentException | NoSuchMethodException | 
				InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("error instantiating the class "+participantClass, e);
		}
		
		stats.update(Event.JPF_START);
		String maudeProcess = getMaudeProcess(participant);
		stats.update(Event.JPF_END);
		
		
		stats.update(Event.MAUDE_START);
		
		HonestyResult honesty;
		
		if (maudeProcess==null) {
			honesty = HonestyResult.UNKNOWN;
		}
		else {
			honesty = new MaudeExecutor(new MaudeConfigurationFromResource()).checkHonesty(maudeProcess);
		}
		stats.update(Event.MAUDE_END);
		
		
		stats.update(Event.HONESTY_END);
		
		printStatistics(stats);
		
		System.out.println("-------------------------------------------------- result");
		System.out.println("honesty: "+honesty);
		System.out.println("==================================================");
		return honesty;
	}
	
	
	
	
	/**
	 * We use JPF to extract the maude code by java code introspection
	 * @param participant
	 * @return
	 */
	private static String getMaudeProcess(Participant participant) {
		
		String processSerialized = ObjectUtils.serializeObjectToStringQuietly(participant);
		
		Config.enableLogging(false);
		Config conf = JPF.createConfig(new String[]{});
		
		loadResourceProperties(conf);		// load configuration from resource files
		configureClasspath(conf);			// configure the classpath of JPF
		configureScheduler(conf);			// configure scheduler to state generation when creating a new thread
		configureNhandler(conf);			// configure JPF-nhandler
		setTarget(conf, processSerialized);	// configure the SUT (system under test) 
		handleCustomProperties(conf);		// handle custom properties (set other JPF properties)
		

		JPF jpf = new JPF(conf);
		
		MaudeListener maudeListener = new MaudeListener(conf, participant.getClass());
		jpf.addListener(maudeListener);
		
		try {
			System.out.println("starting JPF to build maude process");
			System.out.println("--------------------------------------------------");
			jpf.run();
			
			
			if (jpf.foundErrors()){
				// ... process property violations discovered by JPF
				System.out.println("JPF found an error");
				
				gov.nasa.jpf.Error error = jpf.getLastError();
				
				System.out.println("error details: "+error.getDetails());

				System.out.println("impossible to build maude process");
				System.out.println("--------------------------------------------------");
				return null;
			}
			else {
				System.out.println("JPF ends without errors");
				
				//jpf.getListenerOfType(MaudeListener.class);
				System.out.println("CO2 maude process:");
				System.out.println("    "+maudeListener.getCo2Process().toMaude("    "));
				
				System.out.println("CO2 maude contracts:");
				
				for (ContractDefinition c : maudeListener.getContracts().values()) {
					System.out.println("    "+c.getName()+": "+c.getContract().toMaude());
				}
				
				System.out.println("CO2 maude defined process:");
				for (ProcessDefinitionDS p : maudeListener.getEnvProcesses()) {
					System.out.println("    "+p.toMaude("    "));
				}
				
				String maudeProcess = MaudeTemplate.getMaudeProcess(maudeListener);
				
				return maudeProcess;
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
		
		System.out.println("Unexpected error occurs");
		return null;
	}
	
	

	/**
	 * JPF starting-point
	 * @param serializedParticipant
	 */
	public static void runProcess(String[] serializedParticipant) {
		System.out.println("serializedParticipant: "+serializedParticipant[0]);
		
		Participant p = ObjectUtils.deserializeObjectFromStringQuietly(serializedParticipant[0], Participant.class);
		p.run();
	}
	
	
	private static void printStatistics(Statistics stats) {
		System.out.println("-------------------------------------------------- statistics");
		System.out.println(getStatisticString("Build of maude process: ", stats.getJPFTime()));
		System.out.println(getStatisticString("Model-check of maude process: ", stats.getMaudeTime()));
		System.out.println(getStatisticString("Total time spent: ", stats.getTotalTime()));
	}
	
	private static String getStatisticString(String prefix, long time) {
		prefix = StringUtils.rightPad(prefix, 30);
		String timeString = StringUtils.leftPad(time+" msec", 20);
		return prefix+timeString;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static void loadResourceProperties(Config conf) {
		
		try (
				InputStream jpfCoreProps = HonestyChecker.class.getResourceAsStream("/jpf-core.properties");
				InputStream jpfNhandlerProps = HonestyChecker.class.getResourceAsStream("/jpf-nhandler.properties");
				InputStream co2Props = HonestyChecker.class.getResourceAsStream("/co2.properties");
				InputStream localProps = HonestyChecker.class.getResourceAsStream("/local.properties");
				)
		{
			conf.load(jpfCoreProps);
			conf.load(jpfNhandlerProps);
			conf.load(co2Props);
			
			if (localProps!=null) {		//not mandatory
				System.out.println("loading local properties");
				conf.load(localProps);
			}
		}
		catch (IOException e1) {
			throw new RuntimeException("unable to load the jpf config file", e1);
		}
		
	}
	
	private static void configureClasspath(Config conf) {
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
					(a,b) -> {
						if (a.equals("co2apiHL.jar")) {
			                return (b.equals("co2apiHL.jar")) ? 0 : 1;
			            } 
						else if (b.contains("co2apiHL.jar")) {
			                return -1;
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
	}
	
	private static void configureScheduler(Config conf) {
		conf.setProperty("vm.scheduler.class", "gov.nasa.jpf.vm.DelegatingScheduler");
		conf.setProperty("vm.scheduler.sync.class",  "it.unica.co2.honesty.CO2SyncPolicy");
		conf.setProperty("vm.scheduler.sharedness.class", "it.unica.co2.honesty.CO2SharednessPolicy");
	}
	
	private static void configureNhandler(Config conf) {
		
		try {
			File nHandlerTmpDir = Files.createTempDirectory("co2-nhandler").toFile();
			new File(nHandlerTmpDir, "onthefly").mkdir();
			
			conf.setProperty("jpf-nhandler", nHandlerTmpDir.getAbsolutePath());
			conf.setProperty("jpf-nhandler.native_classpath", null);
			conf.setProperty("jpf-nhandler.classpath", null);
			conf.setProperty("jpf-nhandler.test_classpath", null);
			conf.setProperty("jpf-nhandler.sourcepath", null);
			
			conf.setProperty("nhandler.spec.delegate", "it.unica.co2.api.contract.Contract.toTST");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	private static void setTarget(Config conf, String processSerialized) {
		conf.setTarget(HonestyChecker.class.getName());
		conf.setTargetEntry("runProcess([Ljava/lang/String;)V");
		conf.setTargetArgs(new String[]{processSerialized});
	}


	private static void handleCustomProperties(Config conf) {
		if (!conf.getBoolean("honesty.print_SUT_output", false)) {
			//disable SUT logs
			conf.setProperty("vm.tree_output", "false");
		}
		
		if (!conf.getBoolean("honesty.print_JPF_output", false)) {
			//disable JPF logs
			conf.setProperty("report.console.constraint", "constraint,snapshot");
			conf.remove("report.console.finished");
			conf.remove("report.console.probe");
			conf.remove("report.console.property_violation"); 
			conf.remove("report.console.start"); 
			conf.remove("report.console.transition"); 
		}
		
		if (conf.getBoolean("honesty.print_JPF_properties", false))
			conf.printEntries();
	}
	
}
