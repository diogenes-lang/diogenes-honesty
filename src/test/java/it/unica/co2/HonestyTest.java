package it.unica.co2;

import it.unica.co2.examples.APIExampleProcess;
import it.unica.co2.examples.Buyer;
import it.unica.co2.examples.MultipleIfThenElseProcess;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.model.process.Participant;

import org.junit.Test;


public class HonestyTest {

	@Test
	public void test() {
		
		Participant p = new APIExampleProcess();
		
		HonestyChecker.isHonest(p);
	}
	
	@Test
	public void buyer() {
		
		Participant p = new Buyer();
		HonestyChecker.isHonest(p);
	}
	
	@Test
	public void multipleIfThenElse() {
		
		Participant p = new MultipleIfThenElseProcess();
		HonestyChecker.isHonest(p);
	}
}
