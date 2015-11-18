package it.unica.co2.api.contract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface InputPatterns {

	public static final String base64 =  "[a-z0-9A-Z]{22}==";
	public static final String intNumber = "[0-9]{1,9}";
	public static final String longNUmber = "[0-9]{10,18}";
	
	String[] value();

}
