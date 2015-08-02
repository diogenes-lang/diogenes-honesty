package it.unica.co2.honesty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
	
	private String getProperty(String key, String defaultValue) {
		return prop.getProperty(key, defaultValue);
	}
	
	private Boolean getBooleanProperty(String key, Boolean defaultValue) {
		return Boolean.valueOf(prop.getProperty(key, defaultValue.toString()));
	}
	
	private File getFileProperty(String key, String defaultPath) {
		String path = getProperty(key, defaultPath);
		
		File file = Paths.get(path).toFile();
		boolean exist = file.exists();
		
		if (exist) {
			//the path is absolute
			return file;
		}
		else {
			//not exists, search for a relative path into resources
			URL resource = MaudeConfigurationFromResource.class.getResource(path);
			
			try {
				file = Paths.get(resource.getFile()).toFile();
			}
			catch (Exception e) {
				
				if (path.equals(defaultPath))
					throw new IllegalStateException("invalid path "+path, e);
				else {
					//try default path
					return getFileProperty(defaultPath, defaultPath);
				}
			}
			return file;
		}
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
		File file = getFileProperty("honesty.maude.co2-maude", "/co2-maude");
		Validate.isTrue(file.isDirectory(), "file "+file+" not exists or is not a directory");
		return file;
	}
	
	private File getMaudeDir() {
		File file = getFileProperty("honesty.maude.dir", "/maude");
		Validate.isTrue(file.isDirectory(), "file "+file+" not exists or is not a directory");
		return file;
	}
	
	@Override
	public File getMaudeExec() {
		File file = new File(getMaudeDir(), resolveMaudeExecFilename());
		Validate.isTrue(file.isFile(), "file "+file+" not exists or is not a file");
		return file;
	}
	
	private String resolveMaudeExecFilename() {
		
		String execName = getProperty("honesty.maude.exec");
		
		if (execName!=null)
			return execName;
		
		String arch = System.getProperty("os.arch");
		Validate.notNull(arch, "failed to get property 'os.arch'");
		
		if (arch.equals("x86")) {
			return "maude.linux";
		}
		else {
			return "maude.linux64";
		}
		
	}
}
