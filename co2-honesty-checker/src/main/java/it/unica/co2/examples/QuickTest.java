package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.SessionI;
import co2api.TST;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SerializableConsumer;
import it.unica.co2.honesty.HonestyChecker;

public class QuickTest extends Participant {

	private static final long serialVersionUID = 1L;
	
	public QuickTest() {
		super("nicola@co2.unica.it", "nicola");
	}

	public static void main(String[] args) throws ContractException {
		HonestyChecker.isHonest(QuickTest.class);
//		new QuickTest().run();
	}
	
	@Override
	public void run() {
		
			ContractDefinition c1 = def("c1").setContract(
					externalSum()
					.add("a")
					.add("b")
				);
			
//			ContractDefinition c2 = def("c2").setContract(
//					externalSum()
//					.add("c")
//					.add("d")
//				);
			
			System.out.println(c1.getContract().toTST());
			System.out.println(c1.getContract().toTST());
			
			
			try {
				SessionI<TST> x = tellAndWait(c1, 5000);
				try {
					SessionI<TST> y = tellAndWait(c1, 5000);
					y.sendIfAllowed("fooyy");
					x.sendIfAllowed("fooxx");
				}
				catch( ContractExpiredException e) {
					x.sendIfAllowed("foo");
				}
			} 
			catch( ContractExpiredException e) {
				
			}
			
			/*
			 * do x a1? . consume1
			 * + do x b1? . consume1
			 * + do y a2? . consume2
			 * + do y b2? . consume2
			 * + t 
			 */
//			MultipleSessionReceiver mReceiver = 
//					multipleSessionReceiver()
//					.add(x, new ConsumerA(), "a")
//					.add(x, new ConsumerB(), "b")
//					.add(y, new ConsumerC(), "c")
//					.add(y, new ConsumerD(), "d");
//			
//			try {
//				mReceiver.waitForReceive(10_000);
//			}
//			catch (TimeExpiredException e) {
//				
//			}
			
			// no code for JPF
			
	}
	
	
	private static class ConsumerA implements SerializableConsumer<Message> {

		@Override
		public void accept(Message t) {
			System.out.println("msg A received");
		}
		
	}
	
	private static class ConsumerB implements SerializableConsumer<Message> {

		@Override
		public void accept(Message t) {
			System.out.println("msg B received");
		}
		
	}
	
	private static class ConsumerC implements SerializableConsumer<Message> {

		@Override
		public void accept(Message t) {
			System.out.println("msg C received");
		}
		
	}
	
	private static class ConsumerD implements SerializableConsumer<Message> {

		@Override
		public void accept(Message t) {
			System.out.println("msg D received");
		}
		
	}
}
