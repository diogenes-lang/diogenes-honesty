package it.unica.co2.api;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractModel;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;


public class Session2<T extends ContractModel> extends Session<T>{

	private static int count = 0;
	
	private final String sessionName;
	
	public Session2(CO2ServerConnection conn, Public<T> publ) {
		super(conn, publ);
		sessionName = "x_"+count++;
	}
	
	public String getSessionName() {
		return sessionName;
	}
	
	public static String getNextSessionName() {
		return "x_"+count;
	}
	
	
	@Override
	public Boolean send(String action) {
    	System.out.println(">>> sending "+action+"!");

		try {
			return super.send(action);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean send(String action, String value) {
		System.out.println(">>> sending "+action+"! ["+value+"]");

		try {
			return super.send(action, value);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean send(String action, Integer value) {
		System.out.println(">>> sending "+action+"! ["+value+"]");
		
		try {
			return super.send(action, value);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public Message waitForReceive(String... labels) {
		return waitForReceive(-1, labels);
	}
	
	public Message waitForReceive(Integer msec, String... labels) throws TimeExpiredException {
		System.out.println("*** listening for "+ Arrays.toString(labels));
		
		try {
			/*
			 * instruct the dummy CO2ServerConnection to return this labels
			 */
			Field actionsField = CO2ServerConnection.class.getDeclaredField("actions");
			
			Set<String> actionsSet = new HashSet<String>(Arrays.asList(labels));
			
			actionsField.setAccessible(true);
			actionsField.set(null, actionsSet);
		}
		catch (NoSuchFieldException e) {
			// you are using the real implementation, this is not an error
			System.out.println("real implementation");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
			
		
		while(true) {
			// the super.waitForReceive is blocking, delay is not necessary
			// skip all msg with unexpected labels
			
			Message msg;
			try {
				if (msec.equals(-1))
					msg = super.waitForReceive();
				else
					msg = super.waitForReceive(msec);
			}
			catch (ContractException e) {
				throw new RuntimeException(e);
			}
			
			String label = msg.getLabel();
			
			for (String l : labels) {
				if (l.equals(label)) {
					System.out.println("<<< received "+ msg.getLabel()+"?");
					return msg;
				}
			}
		}
	}
	
	
	@Override
	public Boolean amICulpable() {
		
		try {
			return super.amICulpable();
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean amIOnDuty() {
		
		try {
			return super.amIOnDuty();
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}

	
	
}
