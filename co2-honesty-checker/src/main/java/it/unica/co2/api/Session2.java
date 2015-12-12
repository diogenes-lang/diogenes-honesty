package it.unica.co2.api;

import java.util.Arrays;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractModel;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;


public class Session2<T extends ContractModel> extends Session<T>{

	public Session2(CO2ServerConnection conn, Public<T> publ) {
		super(conn, publ);
	}
	
	public Boolean sendIfAllowed(String action) {
    	System.out.println(">>> sending "+action+"!");

		try {
			return super.sendIfAllowed(action);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean sendIfAllowed(String action, String value) {
		System.out.println(">>> sending "+action+"! ["+value+"]");

		try {
			return super.sendIfAllowed(action, value);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean sendIfAllowed(String action, Integer value) {
		System.out.println(">>> sending "+action+"! ["+value+"]");
		
		try {
			return super.sendIfAllowed(action, value);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public Message waitForReceive(String... labels) {
		try {
			return waitForReceive(-1, labels);
		}
		catch (TimeExpiredException e) {
			/* you never go here */
			throw new RuntimeException(e);
		}
	}
	
	public Message waitForReceive(Integer msec, String... labels) throws TimeExpiredException {
		System.out.println("*** listening for "+ Arrays.toString(labels));
		
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
			catch (ContractException | ContractViolationException e) {
				throw new RuntimeException(e);
			}
			
			String label = msg.getLabel();
			
			for (String l : labels) {
				if (l.equals(label)) {
					try {
						System.out.println("<<< received "+ msg.getLabel()+"? ["+msg.getStringValue()+"]");
					}
					catch (ContractException e) {
						System.out.println("<<< received "+ msg.getLabel()+"? [unknown]");
					}
					return msg;
				}
			}
			
			System.out.println("<<< received unexpected "+ msg.getLabel()+"? (unhandled)");
		}
	}
	
	
}
