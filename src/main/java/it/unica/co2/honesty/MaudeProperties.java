package it.unica.co2.honesty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class MaudeProperties {

	private static final Properties prop = new Properties();
	
	static {

		try (
				InputStream in = HonestyChecker.class.getResourceAsStream("/maude.properties");
				)
		{
			prop.load(in);
		}
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	public static String getProperty(String key) {
		return prop.getProperty(key);
	}
	
	public static String getProperty(String key, String defaultValue) {
		return prop.getProperty(key, defaultValue);
	}
	
	public static Boolean getBooleanProperty(String key) {
		return prop.getProperty(key)!=null? Boolean.valueOf(prop.getProperty(key)): null;
	}
	
	public static Boolean getBooleanProperty(String key, Boolean defaultValue) {
		return Boolean.valueOf(prop.getProperty(key, defaultValue.toString()));
	}
}
