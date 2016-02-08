package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Sort.StringSort;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.HonestyChecker;


public class Store1 extends Participant {
	
	final static Logger logger = LoggerFactory.getLogger(Store1.class);

	private static final long serialVersionUID = 1L;

	private static Contract CA = 
			externalSum()
			.add("order", StringSort.string("this is the order"), "{x<60; x}",
					internalSum()
					.add("amount", StringSort.string("0"), "{x<30; x}",
							externalSum().add("pay", "{x<120}"))
					.add("abort", "{x<30}"))
			.setContext("project");
	
	private static Contract CI = 
			internalSum()
			.add("req", "{x<10; x}", 
					externalSum()
					.add("ok", "{x<30}")
					.add("no", "{x<30}"))
			.setContext("project");
	
	protected Store1(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		try {

			Session<TST> session = tellAndWait(CA, 5*60_000);
		
			Message msg = session.waitForReceive("order");
		
			String order = msg.getStringValue();
			
			int amount = getOrderAmount(order);
			
			if (amount<100) {
//				processCall(HandlePayment.class, username, password, session, amount);
				session.sendIfAllowed("amount", amount);
				session.waitForReceive("pay");
			}
			else {
//				processCall(HandleInsurance.class, username, password, session, amount);
				Public<TST> pblI = tell(CI, 5000);
				
				Session<TST> sessionI;
				
				try {
					
					sessionI = pblI.waitForSession();
					sessionI.sendIfAllowed("req", amount);
					
					try {
						msg = sessionI.waitForReceive(10000, "ok", "no");
						
						switch (msg.getLabel()) {
						
						case "ok":
							session.sendIfAllowed("amount", amount);
							session.waitForReceive("pay");
							break;
							
						case "no":
							session.sendIfAllowed("abort");
							break;
						}
						
					}
					catch (TimeExpiredException e) {
						
						session.sendIfAllowed("abort");
						
						sessionI.waitForReceive("ok", "no");
					}
					
				}
				catch (ContractExpiredException e) {
					session.sendIfAllowed("abort");
				}
			}
		}
		catch (ContractExpiredException e) {
		}
		
	}

	
	@SkipMethod
	private int getOrderAmount(String order) {

		int amount;
		
		try {
			amount = Integer.parseInt(order.substring(2));
		}
		catch (NumberFormatException e) {
			amount = new Random().nextInt(100)+50;
		}

		return amount;
	}
	
	
	private static class HandlePayment extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session<TST> session;
		private final Integer amount;
		
		protected HandlePayment(String username, String password, Session<TST> session, Integer amount) {
			super(username, password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			session.sendIfAllowed("amount", amount);
			
			Message msg = session.waitForReceive("pay");
			
		}
	
	}
	
	
	private static class HandleInsurance extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session<TST> session;
		private final Integer amount;
		
		protected HandleInsurance(String username, String password, Session<TST> session, Integer amount) {
			super(username, password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			Public<TST> pblI = tell(CI, 5000);
		
			Session<TST> sessionI;
			
			try {
				
				sessionI = pblI.waitForSession();
				sessionI.sendIfAllowed("req", amount);
				
				try {
					Message msg = sessionI.waitForReceive(10000, "ok", "no");
					
					switch (msg.getLabel()) {
					
					case "ok":
						processCall(HandlePayment.class, username, password,  session, amount);
						break;
						
					case "no":
						session.sendIfAllowed("abort");
						break;
					}
					
				}
				catch (TimeExpiredException e) {
					
					session.sendIfAllowed("abort");
					
					sessionI.waitForReceive("ok", "no");
				}
				
			}
			catch (ContractExpiredException e) {
				session.sendIfAllowed("abort");
			}
		
		}
	
	}
	
	public static void main(String[] args) {
//		new Store("nicola@co2.unica.it", "nicola").run();
		HonestyChecker.isHonest(Store1.class, "", "");
	}
}
