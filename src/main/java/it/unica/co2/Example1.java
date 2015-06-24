package it.unica.co2;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Sort;
import it.unica.co2.model.process.Participant;

public class Example1 extends Participant {
	
	private static String usernameAlice = "alice@test.com";
	private static String passwordAlice = "alice";

	protected Example1() {
		super(usernameAlice, passwordAlice);
	}
	
	@Override
	public void run() {
		
		/*
		 * process {
		 * 		(x) tell x (amount! int . ok? . item! + abort?) . 
		 * 			do x amount! 50 .
		 * 			(
		 * 				do x ok? . do x item!
		 * 				+ do x abort?
		 * 			) 
		 * }
		 */

		Contract contract = 
				internalSum(
						"amount",
						Sort.INT,
						externalSum("ok", internalSum("item"))
							.add(externalAction("abort"))
				);
		
		Session2 session = tell(contract);
//		
//		doSend(session, "amount", 42);
//		
//		doReceiveSum(session)
//			.add(
//					action("ok").next( ()-> {doSend(session, "item");} )
//					)
//			.add(action("abort"))
//			.run();
//		
//		System.out.println("amICulpable: "+amICulpable(session));
//		System.out.println("amIOnDuty: "+amIOnDuty(session));
	}
	
	
	@Override
	protected String getUsername() {
		return usernameAlice;
	}
	
	public static void main (String[] args) throws Exception {
		
		System.out.println("---------Example1---------");
		new Example1().run();
		System.out.println("---------Example1 <END>---------");
	}
}
