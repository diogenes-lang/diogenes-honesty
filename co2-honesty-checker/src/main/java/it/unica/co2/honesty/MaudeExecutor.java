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
import java.util.concurrent.TimeUnit;


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
		
		if (!checkMaudeConfiguration()) {
			//configuration error
			return HonestyResult.UNKNOWN;
		}
		
		File tmpFile = new File(configuration.getCo2MaudeDir(), sdf.format(new Date())+"_java_honesty.maude");
		
		List<String> command = new ArrayList<String>();
		command.add(configuration.getMaudeExec().getAbsolutePath());
		command.add("-no-wrap");
		command.add("-no-advise");
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
			boolean terminated = pr.waitFor(10, TimeUnit.SECONDS);		//wait until the process terminate or the timeout has expired
			
			if (!terminated) {
				out.println("-------------------------------------------------- error");
				out.println("the process is running more than 10 sec, kill");
				pr.destroyForcibly();
				return HonestyResult.UNKNOWN;
			}
			
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
			
			return manageOutput( output.toString());
			
		}
		catch (Exception e) {
			out.println("-------------------------------------------------- error");
			e.printStackTrace(out);
			return HonestyResult.UNKNOWN;
		}
		finally {
			if (configuration.isDeleteTempFile())
				tmpFile.delete();
		}
		
	}
	
	
	private boolean checkMaudeConfiguration() {
		
		if (!configuration.getMaudeExec().isFile()) {
			out.println("-------------------------------------------------- error");
			out.println("invalid path '"+configuration.getMaudeExec()+"', check your configuration");
			return false;
		}
		
		if (!configuration.getCo2MaudeDir().isDirectory()) {
			out.println("-------------------------------------------------- error");
			out.println("invalid path '"+configuration.getMaudeExec()+"', check your configuration");
			return false;
		}
		
		File co2AbsFile = new File(configuration.getCo2MaudeDir(), "co2-abs.maude");
		if (!co2AbsFile.exists()) {
			out.println("-------------------------------------------------- error");
			out.println("file '"+co2AbsFile+"' not found, check your configuration");
			return false;
		}
		
		return true;
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
			out.println();
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
