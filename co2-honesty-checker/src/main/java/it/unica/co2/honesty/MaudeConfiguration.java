package it.unica.co2.honesty;

import java.io.File;

/**
 * The configuration used by {@link MaudeExecutor}.
 * 
 * @see MaudeConfigurationFromResource
 * 
 * @author Nicola Atzei
 */
public interface MaudeConfiguration {

	/**
	 * If you want to print the input maude process that {@link MaudeConfiguration#getMaudeExec()} will check, set to true.
	 * 
	 * @return true if the input maude process will be printed, false otherwise.
	 */
	public boolean showInput();
	
	/**
	 * If you want to print the output stream resulting from the execution of {@link MaudeConfiguration#getMaudeExec()}, set to true.
	 * 
	 * @return true if the output stream will be printed, false otherwise.
	 */
	public boolean showOutput();
	
	/**
	 * Return the directory that contains all necessary maude files to check the honesty of a 
	 * CO2 maude process.
	 * 
	 * @return the co2-maude directory
	 */
	public File getCo2MaudeDir();
	
	/**
	 * @return the maude executable file
	 */
	public File getMaudeExec();
	
	/**
	 * The {@link MaudeExecutor} create a temporary file into {@link MaudeConfiguration#getCo2MaudeDir()}.
	 * 
	 * @return true if the will be deleted after execution, false otherwise.
	 */
	public boolean isDeleteTempFile();

}