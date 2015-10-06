package it.unica.co2.honesty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.lang3.Validate;

public class MaudeConfigurationFromResource implements MaudeConfiguration {

	
	public static final String HONESTY_MAUDE_CO2_MAUDE = "honesty.maude.co2-maude";
	public static final String HONESTY_MAUDE_EXEC = "honesty.maude.exec";
	private final Properties prop = new Properties();
	
	public MaudeConfigurationFromResource() {

		try (
				InputStream co2Props = MaudeConfigurationFromResource.class.getResourceAsStream("/co2.properties");
				InputStream localProps = MaudeConfigurationFromResource.class.getResourceAsStream("/local.properties");
				)
		{
			prop.load(co2Props);
			
			if (localProps!=null) {		//not mandatory
				System.out.println("loading local properties");
				prop.load(localProps);
			}
			
			String sysPropMaudeExec = System.getProperty(HONESTY_MAUDE_EXEC);
			String sysPropMaudeCo2 = System.getProperty(HONESTY_MAUDE_CO2_MAUDE);
			
			if (sysPropMaudeExec!=null)
				prop.setProperty(HONESTY_MAUDE_EXEC, sysPropMaudeExec);
			
			if (sysPropMaudeCo2!=null)
				prop.setProperty(HONESTY_MAUDE_CO2_MAUDE, sysPropMaudeCo2);
		}
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	private String getProperty(String key) {
		return prop.getProperty(key);
	}
	
	private Boolean getBooleanProperty(String key, Boolean defaultValue) {
		return Boolean.valueOf(prop.getProperty(key, defaultValue.toString()));
	}
	
	private int getIntProperty(String key, int defaultValue) {
		return Integer.valueOf(prop.getProperty(key, String.valueOf(defaultValue)));
	}
	
	private File getFileProperty(String key) {
		
		String path = getProperty(key);
		Validate.notNull(path, "the property "+key+" is mandatory");
		
		File file = Paths.get(path).toFile();
		Validate.isTrue(file.exists(), "invalid property '"+key+"': the file '"+file+"' does not exist");
		
		return file;
	}
	
	
	@Override
	public boolean isDeleteTempFile() {
		return getBooleanProperty("honesty.maude.delete_temp_file", true);
	}
	
	@Override
	public boolean showInput() {
		return getBooleanProperty("honesty.maude.verbose", false);
	}
	
	@Override
	public boolean showOutput() {
		return getBooleanProperty("honesty.maude.verbose", false);
	}
	
	@Override
	public File getCo2MaudeDir() {
		File file = getFileProperty(HONESTY_MAUDE_CO2_MAUDE);
		Validate.isTrue(file.isDirectory(), "file '"+file+"' is not a directory");
		return file;
	}
	
	@Override
	public File getMaudeExec() {
		File file = getFileProperty(HONESTY_MAUDE_EXEC);
		Validate.isTrue(file.isFile(), "file '"+file+"' is not a file");
		return file;
	}
	
	@Override
	public int timeout() {
		return this.getIntProperty("honesty.maude.timeout", 10);
	}
}
