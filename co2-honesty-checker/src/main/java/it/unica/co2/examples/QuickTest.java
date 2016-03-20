package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.ContractViolationException;
import co2api.SessionI;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.Participant;
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
			
			ContractDefinition c2 = def("c2").setContract(
					externalSum()
					.add("c")
					.add("d")
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
			
			parallel(()->{
				x.waitForReceive("a", "b", "c");
			});

			parallel(()->{
				y.sendIfAllowed("Y");
			});
			
			parallel(()->{
				try {
					x.waitForReceive(10_000, "END1", "END2");
				}
				catch (ContractException | ContractViolationException | TimeExpiredException e) {
	
				}
			});
	}
	
}
