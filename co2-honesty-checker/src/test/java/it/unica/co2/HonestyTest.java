package it.unica.co2;

import static org.junit.Assert.*;
import it.unica.co2.examples.APIExampleProcess;
import it.unica.co2.examples.MultipleIfThenElseProcess;
import it.unica.co2.examples.ParallelProcessExample.ParallelProcess;
import it.unica.co2.examples.ProcessComposition2Example.Composed2Process;
import it.unica.co2.examples.ProcessCompositionExample.ComposedProcess;
import it.unica.co2.examples.SimpleBuyer;
import it.unica.co2.examples.blackjack.Dealer;
import it.unica.co2.examples.blackjack.DeckService;
import it.unica.co2.examples.blackjack.Player;
import it.unica.co2.examples.ebookstore.Buyer;
import it.unica.co2.examples.ebookstore.Distributor;
import it.unica.co2.examples.ebookstore.Seller;
import it.unica.co2.examples.voucher.VoucherSeller;
import it.unica.co2.honesty.HonestyChecker;

import org.junit.Ignore;
import org.junit.Test;


public class HonestyTest {

	@Test
	public void apiExample() {
		
		boolean honesty = HonestyChecker.isHonest(APIExampleProcess.class);
		
		assertTrue(honesty);
	}
	
	@Test
	public void simpleBuyer() {
		
		boolean honesty = HonestyChecker.isHonest(SimpleBuyer.class);
		
		assertTrue(honesty);
	}
	
	@Test
	public void multipleIfThenElse() {
		
		boolean honesty = HonestyChecker.isHonest(MultipleIfThenElseProcess.class);
		
		assertTrue(honesty);
	}
	
	@Test
	public void composition() {
		
		boolean honesty = HonestyChecker.isHonest(ComposedProcess.class);
		
		assertTrue(honesty);
	}
	
	@Test
	public void composition2() {
		
		boolean honesty = HonestyChecker.isHonest(Composed2Process.class);
		
		assertTrue(honesty);
	}
	
	@Test
	@Ignore
	public void parallel() {
		
		boolean honesty = HonestyChecker.isHonest(ParallelProcess.class);
		
		assertTrue(honesty);
	}
	
	@Test
	public void ebookstore() {
		
		boolean honesty;
		
		honesty = HonestyChecker.isHonest(Buyer.class);
		assertTrue(honesty);
		
		honesty = HonestyChecker.isHonest(Distributor.class);
		assertTrue(honesty);
		
		honesty = HonestyChecker.isHonest(Seller.class);
		assertTrue(honesty);
	}
	
	@Test
	public void blackjack() {
		
		boolean honesty;
		
		honesty = HonestyChecker.isHonest(Dealer.class);
		assertTrue(honesty);
		
		honesty = HonestyChecker.isHonest(DeckService.class);
		assertTrue(honesty);
		
		honesty = HonestyChecker.isHonest(Player.class);
		assertTrue(honesty);
	}
	
	@Test
	public void voucher() {
		
		boolean honesty;
		
		honesty = HonestyChecker.isHonest(VoucherSeller.class);
		assertTrue(honesty);
	}
}
