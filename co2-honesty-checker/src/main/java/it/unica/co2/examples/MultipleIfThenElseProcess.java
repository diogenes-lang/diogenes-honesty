package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;

public class MultipleIfThenElseProcess extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public MultipleIfThenElseProcess() {
		super(username, password);
	}

	public static void main(String[] args) throws ContractException {
		new MultipleIfThenElseProcess().run();
	}
	
	@Override
	public void run() {
		try {
			
			System.out.println("START");

			Contract A = internalSum().add("start",
							internalSum()
								.add("then", 
										internalSum()
											.add("then_1", 
													internalSum().add("end")
											)
											.add("else_1", 
													internalSum().add("end")
											)
								)
								.add("else", 
										internalSum().add("end")
								)
						);
			
			Session<TST> session = tellAndWait(A);
			
			session.sendIfAllowed("start");
			
			Integer x = new Integer(10);
			Integer y = 10;
			
			if (x>5){
				session.sendIfAllowed("then");
				
				if(y>5) {
					session.sendIfAllowed("then_1");
				}
				else {
					session.sendIfAllowed("else_1");
				}
				
			}			
			else {
				session.sendIfAllowed("else");
			}
			
			session.sendIfAllowed("end");
			
			System.out.println("END");
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
