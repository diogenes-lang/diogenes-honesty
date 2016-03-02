package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.SessionI;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.MultipleSessionReceiver;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class QuickTest extends Participant {

	private static final long serialVersionUID = 1L;
	
	public QuickTest() {
		super("alice@test.com", "alice");
	}

	public static void main(String[] args) throws ContractException {
		HonestyChecker.isHonest(QuickTest.class);
		
		System.out.println(new Session<>(null, null) instanceof SessionI);
	}
	
	@Override
	public void run() {
		
			ContractDefinition c1 = def("c1").setContract(
					externalSum()
					.add("a1")
					.add("b1")
				);
			
			ContractDefinition c2 = def("c2").setContract(
					externalSum()
					.add("a2")
					.add("b2")
				);
			
			SessionI<TST> x = tell(c1);
			SessionI<TST> y = tell(c2);
			 
			/*
			 * do x a1? . consume1
			 * + do x b1? . consume1
			 * + do y a2? . consume2
			 * + do y b2? . consume2
			 * + t 
			 */
			MultipleSessionReceiver mReceiver = 
					multipleSessionReceiver()
					.add(x, this::consumeA1, "a1")
					.add(x, this::consumeB1, "b1")
					.add(y, this::consumeA2, "a2")
					.add(y, this::consumeB2, "b2");
			
			try {
				mReceiver.waitForReceive(10_000);
			}
			catch (TimeExpiredException e) {
				
			}
			
			// no code for JPF
			
	}
	
	
	private void consumeA1(Message msg) {
		
	}
	
	private void consumeA2(Message msg) {
		
	}
	
	private void consumeB1(Message msg) {
		
	}
	
	private void consumeB2(Message msg) {
		
	}
}
