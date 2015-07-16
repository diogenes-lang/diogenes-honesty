package it.unica.co2.honesty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;


public class MaudeExecutor {

	private static final boolean DELETE_MAUDE_TMP_FILE;
	private static final String MAUDE_EXEC;
	private static final String CO2_MAUDE_DIR;
	private static final boolean VERBOSE;
	
	static {
		
		MAUDE_EXEC = MaudeProperties.getProperty("maude.exec");
		CO2_MAUDE_DIR = MaudeProperties.getProperty("maude.co2-maude");
		DELETE_MAUDE_TMP_FILE = MaudeProperties.getBooleanProperty("maude.delete_temp_file", true);
		VERBOSE = MaudeProperties.getBooleanProperty("maude.verbose", false);
		
		Validate.notNull(MAUDE_EXEC, "property 'maude.exec' is mandatory");
		Validate.notNull(CO2_MAUDE_DIR, "property 'maude.co2-maude' is mandatory");
	}
	
	public static boolean invokeMaudeHonestyChecker(String process) {
		
		System.out.println("--------------------------------------------------");
		System.out.println("model checking the maude process");
		
		File maudeExecutable = new File(MAUDE_EXEC);
		File co2MaudeDir = new File(CO2_MAUDE_DIR);
		
		Validate.isTrue(maudeExecutable.isFile());
		Validate.isTrue(co2MaudeDir.isDirectory());
		
		File tmpFile = new File(co2MaudeDir, System.currentTimeMillis()+"_java_honesty.maude");
		
		List<String> command = new ArrayList<String>();
		command.add(maudeExecutable.getAbsolutePath());
		command.add(tmpFile.getAbsolutePath());
		
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(co2MaudeDir);
		pb.redirectErrorStream(true);
		
		try {
			
			System.out.println("................................................."+process.getBytes().length);
			
			/*
			 * write the maude code to tempFile
			 */
			try (
					BufferedWriter output = new BufferedWriter(new FileWriter(tmpFile));
			)
			{
				output.write(process);
				output.flush();
			}
			
			//the file is written
			
			Process pr = pb.start();
			pr.waitFor();				//wait until the process stop
			
			/*
			 * read the process output
			 */
			StringBuilder output = new StringBuilder();

			try (
					BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			)
			{
				String line;
				
				while ((line=input.readLine())!=null) {
					output.append(line).append('\n');
				}
			}
			/*
			 * the output is truncated to 80 characters (I don't know why)
			 */
			return manageOutput( output.toString().replace("\n    ", " "));
			
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			if (DELETE_MAUDE_TMP_FILE)
				tmpFile.delete();
		}
		
	}
	
	/**
	 * This method check the presence of warnings/errors into the output of the maude execution. If not found,
	 * search for the presence of honest/dishonest pattern.
	 * @param output
	 * @return true if the process is honest and no warnings/errors were found, false otherwise. 
	 */
	private static boolean manageOutput(String output) {
		
		if (VERBOSE) {
			System.out.println("-------------------------------------------------- maude output");
			System.out.println(output);
		}

		if (checkForWarning(output)) {
			// the maude checking contains warning
			System.out.println("[IMPORTANT] Found some warnings in the maude output: rerun with option maude.verbose=true to see the output");
			return false;
		}
		
		boolean isHonest = checkForHonesty(output);
		boolean isDishonest = checkForDishonesty(output);
		
		assert isHonest==!isDishonest;
		
		return isHonest;
	}
	
	private static final String RESULT_HONEST = "\nresult Bool: true";
	private static final String RESULT_DISHONEST = "\nresult TSystem: ";
	
	private static boolean checkForWarning(String output) {
		return output.contains("\nWarning: ");
	}
	
	private static boolean checkForHonesty(String output) {
		return output.contains(RESULT_HONEST);
	}
	
	private static boolean checkForDishonesty(String output) {
		return output.contains(RESULT_DISHONEST);
	}
}
