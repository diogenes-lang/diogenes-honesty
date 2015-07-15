package it.unica.co2;

import static org.junit.Assert.*;
import it.unica.co2.examples.APIExampleProcess;
import it.unica.co2.examples.MultipleIfThenElseProcess;
import it.unica.co2.examples.ParallelProcessExample.ParallelProcess;
import it.unica.co2.examples.ProcessComposition2Example.Composed2Process;
import it.unica.co2.examples.ProcessCompositionExample.ComposedProcess;
import it.unica.co2.examples.SimpleBuyer;
import it.unica.co2.examples.ebookstore.Buyer;
import it.unica.co2.examples.ebookstore.Distributor;
import it.unica.co2.examples.ebookstore.Seller;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.model.process.Participant;

import org.junit.Ignore;
import org.junit.Test;


public class HonestyTest {

	@Test
	public void apiExample() {
		
		Participant p = new APIExampleProcess();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
	
	@Test
	public void simpleBuyer() {
		
		Participant p = new SimpleBuyer();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
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
	public void composition2() {
		
		Participant p = new Composed2Process();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
	
	@Test
	@Ignore
	public void parallel() {
		
		Participant p = new ParallelProcess();
		boolean honesty = HonestyChecker.isHonest(p);
		
		assertTrue(honesty);
	}
	
	@Test
	public void buyer() {
		
		Participant p = new Buyer();
		boolean honesty = HonestyChecker.isHonest(p);
		assertTrue(honesty);
	}
	
	@Test
	public void distributor() {
		
		Participant p = new Distributor();
		boolean honesty = HonestyChecker.isHonest(p);
		assertTrue(honesty);
	}
	
	@Test
	public void seller() {
		
		Participant p = new Seller();
		boolean honesty = HonestyChecker.isHonest(p);
		assertTrue(honesty);
	}
}
