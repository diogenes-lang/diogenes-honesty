package it.unica.co2.honesty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.Statistics.Event;
import it.unica.co2.util.ObjectUtils;

public class HonestyChecker {
	
	public static Statistics stats;
	
	@SkipMethod
	synchronized public static <T extends Participant> HonestyResult isHonest(Class<T> participantClass, Object... args) {
		
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
		catch (Exception e) {
			
			ctor = ConstructorUtils.getMatchingAccessibleConstructor(participantClass, typesArray);
			try {
				ctor.setAccessible(true);
				participant = ctor.newInstance(args);			// create a new instance passing the given args
			}
			catch (Exception e1) {
				throw new RuntimeException("error instantiating the class "+participantClass, e);
			}			
		}
		
		return isHonest(participant);
	}
	
	@SkipMethod
	synchronized public static <T extends Participant> HonestyResult isHonest(T participant) {
		
		stats = new Statistics();
		stats.update(Event.HONESTY_START);
		
		System.out.println("================================================== HONESTY CHECKER ");
		System.out.println("checking the honesty of "+participant.getClass().getName());
		
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
	
	
	
	@SkipMethod
	public static String getMaudeProcess(Participant participant) {
		
		String processSerialized = ObjectUtils.serializeObjectToStringQuietly(participant);
		
		Config.enableLogging(false);
		Config conf = JPF.createConfig(new String[]{});
		
		loadResourceProperties(conf);		// load configuration from resource files
		configureClasspath(conf);			// configure the classpath of JPF
		configureScheduler(conf);			// configure scheduler to state generation when creating a new thread
//		configureNhandler(conf);			// configure JPF-nhandler
		setTarget(conf, processSerialized);	// configure the SUT (system under test) 
		handleCustomProperties(conf);		// handle custom properties (set other JPF properties)
		

		JPF jpf = new JPF(conf);
		
		Co2Listener co2Listener = new Co2Listener(conf, participant.getClass());
		jpf.addListener(co2Listener);
		
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
//				System.out.println("CO2 maude process:");
//				System.out.println("    "+co2Listener.getCo2Process().toMaude("    "));
//				
//				System.out.println("CO2 maude contracts:");
//				
//				for (ContractDefinition c : co2Listener.getContracts().values()) {
//					System.out.println("    "+c.getName()+": "+c.getContract().toMaude());
//				}
//				
//				System.out.println("CO2 maude defined process:");
//				for (ProcessDefinitionDS p : co2Listener.getEnvProcesses()) {
//					System.out.println("    "+p.toMaude("    "));
//				}
				
				String maudeProcess = MaudeTemplate.getMaudeProcess(co2Listener);
				
				return maudeProcess;
			}
			
		}
		catch (Exception e){
			System.out.println("Unexpected error occurs");
			System.out.println(ExceptionUtils.getStackTrace(e));
			return null;
		}
		
	}
	
	

	/**
	 * JPF starting-point
	 * @param serializedParticipant
	 */
	@SuppressWarnings("unused")
	private static void runProcess(String[] serializedParticipant) {
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
//				InputStream jpfNhandlerProps = HonestyChecker.class.getResourceAsStream("/jpf-nhandler.properties");
				InputStream co2Props = HonestyChecker.class.getResourceAsStream("/co2.properties");
				InputStream localProps = HonestyChecker.class.getResourceAsStream("/local.properties");
				)
		{
			conf.load(jpfCoreProps);
//			conf.load(jpfNhandlerProps);
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
		
		String classpath = System.getProperty("java.class.path");
		
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
	
	@SuppressWarnings("unused")
	private static void configureNhandler(Config conf) {
		
		File nHandlerTmpDir = new File(System.getProperty("java.io.tmpdir"), "co2-nhandler"+new Random().nextLong());
		nHandlerTmpDir.mkdir();
		new File(nHandlerTmpDir, "onthefly").mkdir();
		
		conf.setProperty("jpf-nhandler", nHandlerTmpDir.getAbsolutePath());
		conf.setProperty("jpf-nhandler.native_classpath", null);
		conf.setProperty("jpf-nhandler.classpath", null);
		conf.setProperty("jpf-nhandler.test_classpath", null);
		conf.setProperty("jpf-nhandler.sourcepath", null);
		
		conf.setProperty("nhandler.spec.delegate", "it.unica.co2.api.contract.Contract.toTST");
		conf.setProperty("nhandler.spec.delegate", "co2api.ContractXML.*");
		
		conf.setProperty("nhandler.spec.skip", "co2api.CO2ServerConnection.*");
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
