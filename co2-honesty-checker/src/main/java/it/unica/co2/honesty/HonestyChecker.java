package it.unica.co2.honesty;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import it.unica.co2.honesty.Statistics.Event;
import it.unica.co2.honesty.dto.ProcessDefinitionDTO;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import it.unica.co2.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

public class HonestyChecker {
	
	
	synchronized public static <T extends Participant> HonestyResult isHonest(Class<T> participantClass) {
		
		Statistics.update(Event.HONESTY_START);
		
		System.out.println("================================================== HONESTY CHECKER ");
		System.out.println("checking the honesty of "+participantClass.getName());
		
		Participant participant;
		try {
			participant = participantClass.newInstance();
		}
		catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException("error instatiating the class "+participantClass, e);
		}
		
		Statistics.update(Event.JPF_START);
		String maudeProcess = getMaudeProcess(participant);
		Statistics.update(Event.JPF_END);
		
		
		Statistics.update(Event.MAUDE_START);
		
		HonestyResult honesty;
		
		if (maudeProcess==null) {
			honesty = HonestyResult.UNKNOWN;
		}
		else {
			honesty = new MaudeExecutor(new MaudeConfigurationFromResource()).checkHonesty(maudeProcess);
		}
		Statistics.update(Event.MAUDE_END);
		
		
		Statistics.update(Event.HONESTY_END);
		
		printStatistics();
		
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
		
		conf.setTarget(HonestyChecker.class.getName());
		conf.setTargetEntry("runProcess([Ljava/lang/String;)V");
		conf.setTargetArgs(new String[]{processSerialized});
		
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
				
				for (Entry<String, Contract> c : maudeListener.getContracts().entrySet()) {
					System.out.println("    "+c.getKey()+": "+c.getValue().toMaude());
				}
				
				System.out.println("CO2 maude defined process:");
				for (ProcessDefinitionDTO p : maudeListener.getEnvProcesses()) {
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
	
	
	private static void printStatistics() {
		System.out.println("-------------------------------------------------- statistics");
		System.out.println(getStatisticString("Build of maude process: ", Statistics.getJPFTime()));
		System.out.println(getStatisticString("Model-check of maude process: ", Statistics.getMaudeTime()));
		System.out.println(getStatisticString("Total time spent: ", Statistics.getTotalTime()));
	}
	
	private static String getStatisticString(String prefix, long time) {
		prefix = StringUtils.rightPad(prefix, 30);
		String timeString = StringUtils.leftPad(time+" msec", 20);
		return prefix+timeString;
	}
}
