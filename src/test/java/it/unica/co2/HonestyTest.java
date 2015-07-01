package it.unica.co2;

import static org.junit.Assert.*;
import it.unica.co2.examples.APIExampleProcess;
import it.unica.co2.examples.Buyer;
import it.unica.co2.examples.MultipleIfThenElseProcess;
import it.unica.co2.examples.ParallelProcessExample.ParallelProcess;
import it.unica.co2.examples.ProcessCompositionExample.ComposedProcess;
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
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
	
	@Test
	public void composition() {
		
		Participant p = new ComposedProcess();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
	
	@Test
	public void parallel() {
		
		Participant p = new ParallelProcess();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
}
