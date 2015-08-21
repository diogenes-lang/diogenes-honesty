package it.unica.co2.honesty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.lang3.Validate;

public class MaudeConfigurationFromResource implements MaudeConfiguration {

	private final Properties prop = new Properties();
	
	public MaudeConfigurationFromResource() {
		this("/co2.properties");
	}
	
	public MaudeConfigurationFromResource(String resourcePath) {

		try (
				InputStream in = MaudeConfigurationFromResource.class.getResourceAsStream(resourcePath);
				)
		{
			prop.load(in);
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
		Validate.isTrue(file.exists(), "the property "+key+" point to the not existent file "+file);
		
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
		File file = getFileProperty("honesty.maude.co2-maude");
		Validate.isTrue(file.isDirectory(), "file "+file+" is not a directory");
		return file;
	}
	
	@Override
	public File getMaudeExec() {
		File file = getFileProperty("honesty.maude.exec");
		Validate.isTrue(file.isFile(), "file "+file+" is not a file");
		return file;
	}
	
	@Override
	public int timeout() {
		return this.getIntProperty("honesty.maude.timeout", 10);
	}
}
