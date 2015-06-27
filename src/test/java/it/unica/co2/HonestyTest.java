package it.unica.co2;

import it.unica.co2.examples.APIExampleProcess;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.model.process.Participant;

import org.junit.Test;


public class HonestyTest {

	@Test
	public void test() {
		
		Participant p = new APIExampleProcess();
		
		HonestyChecker.isHonest(p);
	}

}
