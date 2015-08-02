package it.unica.co2.honesty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MaudeExecutor {

	private static final String RESULT_HONEST = "\nresult Bool: true";
	private static final String RESULT_DISHONEST = "\nresult TSystem: ";
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
	
	private PrintStream out;
	
	private MaudeConfiguration configuration;
	
	public MaudeExecutor(MaudeConfiguration maudeProperties) {
		this.configuration = maudeProperties;
		this.out = System.out;
	}
	
	public void redirectOutput(OutputStream out) {
		this.out = new PrintStream(out);
	}

	/**
	 * Check the honesty of the given maude process.
	 * @param process
	 * @return
	 */
	public HonestyResult checkHonesty(String process) {
		
		out.println("--------------------------------------------------");
		out.println("model checking the maude process");
		
		
		
		File tmpFile = new File(configuration.getCo2MaudeDir(), sdf.format(new Date())+"_java_honesty.maude");
		
		List<String> command = new ArrayList<String>();
		command.add(configuration.getMaudeExec().getAbsolutePath());
		command.add(tmpFile.getAbsolutePath());
		
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(configuration.getCo2MaudeDir());
		pb.redirectErrorStream(true);
		
		try {
			
			if (configuration.showInput()) {
				out.println("-------------------------------------------------- maude input process");
				out.println(process);
			}
			
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
			return HonestyResult.UNKNOWN;
		}
		finally {
			if (configuration.isDeleteTempFile())
				tmpFile.delete();
		}
		
	}
	
	/**
	 * This method check the presence of warnings/errors into the output of the maude execution. If not found,
	 * search for the presence of honest/dishonest pattern.
	 * @param output
	 * @return true if the process is honest and no warnings/errors were found, false otherwise. 
	 */
	private HonestyResult manageOutput(String output) {
		
		if (configuration.showOutput()) {
			out.println("-------------------------------------------------- maude output");
			out.println(output);
		}

		if (checkForWarning(output)) {
			// the maude checking contains warning
			return HonestyResult.UNKNOWN;
		}
		
		boolean isHonest = checkForHonesty(output);
		boolean isDishonest = checkForDishonesty(output);
		
		assert isHonest==!isDishonest;
		
		if (isHonest)
			return HonestyResult.HONEST;
		else
			return HonestyResult.DISHONEST;
	}
	
	private boolean checkForWarning(String output) {
		return output.contains("\nWarning: ");
	}
	
	private boolean checkForHonesty(String output) {
		return output.contains(RESULT_HONEST);
	}
	
	private boolean checkForDishonesty(String output) {
		return output.contains(RESULT_DISHONEST);
	}
}
