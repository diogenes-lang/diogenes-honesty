

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Sort;
import it.unica.co2.model.process.Participant;

public class Example2 extends Participant {

	private static String usernameBob = "bob@test.com";
	private static String passwordBob = "bob";

	protected Example2() {
		super(usernameBob, passwordBob);
	}

	@Override
	public void run() {
		
		/*
		 * process {
		 * 		(x) tell x (amount? n:int . ok! . item? + abort!) . 
		 * 			do x amount? n .
		 * 			if (n<50)
		 * 				do x ok!
		 * 				do x item? . 
		 * 			else
		 * 				do x abort!
		 * }
		 */

		Contract contract = 
				externalSum(
						"amount",
						Sort.INT,
						internalSum()
							.add("ok", externalSum("item"))
							.add("abort")
				);
		
		Session2 session = tell(contract);
//		Variable var = new Variable(Sort.INT);
		
//		doReceive(session, var, "amount");
//
//		if (var.getValue(Integer.class)<50) {
//			doSend(session, "ok");
//			doReceive(session, "item");
//		}
//		else {
//			doSend(session, "abort");
//		}
//		
//		System.out.println("message label: "+var.getLabel());
//		System.out.println("message value: "+var.getValue());
//		
//		System.out.println("amICulpable: "+amICulpable(session));
//		System.out.println("amIOnDuty: "+amIOnDuty(session));
	}

	@Override
	protected String getUsername() {
		return usernameBob;
	}

	public static void main (String[] args) throws Exception {
		
		System.out.println("---------Example2---------");
		new Example2().run();
		System.out.println("---------Example2 <END>---------");
	}
}
