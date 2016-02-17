package it.unica.co2.examples;
/**
 * TASK 1
 *
 * nome: Cesare
 * cognome: Sollai
 * matricola: 65021
 *
 * Fondamenti di sicurezza - A.A. 2015-2016
 */

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Session;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;


public class Store0 extends Participant{

	private static final long serialVersionUID = 1L;
	
	private static final String username = "65021@co2.unica.it";
	private static final String password = "65021";
	
	protected Store0() {
		super(username, password);
		// TODO Auto-generated constructor stub
	}

	int price = 50;
	
	// Store's contract
	private Contract c1 = externalSum().add("order", internalSum()
					.add("abort")
					.add("amount", externalSum().add("pay")));
	
	// Insurance's contract
	private Contract c2 = internalSum().add("req", externalSum().add("yes").add("no"));

	@Override
	public void run() {
		Session<TST> s1 = tell(c1).waitForSession();
		s1.waitForReceive("order");

		
		logger.info("foo");
		if (price < 100) { // checking the order's price
			s1.sendIfAllowed("amount", price);
			Message m = s1.waitForReceive("pay");
				// do something with the message (m.getStringValue())
		}
		else {
		
			Public<TST> pbl2 = tell(c2, 10_000); 
			
			try {
			  Session<TST> s2 = pbl2.waitForSession();    // can throws ContractExpiredException
			  s2.sendIfAllowed("req", price);
			  
//			  try {
//			    Message msg = s2.waitForReceive(10_000, "yes", "no"); // can throws TimeExpiredException
//			    
//			    switch(msg.getLabel()) {
//			    	case "yes":
//			    		s1.sendIfAllowed("amount");
//				    	s1.waitForReceive("pay");
//				    	break;
//			    	case "no":
//			    		s1.sendIfAllowed("abort");
//			    		break;
//			    }
//			    
//			  }
//			  catch (TimeExpiredException e) {
				 s1.sendIfAllowed("abort");
//				 s2.waitForReceive("yes", "no");
//			  }  
			  
			} catch (ContractExpiredException e) {
			  s1.sendIfAllowed("abort");
			}
			
		}
	}
	
	public static void main (String[] args) {
        HonestyChecker.isHonest(Store0.class);
        // new Store().run();
	}
	
	
	
}



