package it.unica.co2;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import it.unica.co2.examples.MultipleIfThenElseProcess;
import it.unica.co2.examples.ParallelProcessExample.ParallelProcess;
import it.unica.co2.examples.ProcessComposition2Example.Composed2Process;
import it.unica.co2.examples.ProcessCompositionExample.ComposedProcess;
import it.unica.co2.examples.SimpleBuyer;
import it.unica.co2.examples.blackjack.Dealer;
import it.unica.co2.examples.blackjack.DeckService;
import it.unica.co2.examples.blackjack.Player;
import it.unica.co2.examples.ebookstore.Buyer;
import it.unica.co2.examples.ebookstore.DishonestSeller;
import it.unica.co2.examples.ebookstore.Distributor;
import it.unica.co2.examples.ebookstore.Seller;
import it.unica.co2.examples.insuredsale.IBuyer;
import it.unica.co2.examples.insuredsale.Insurance;
import it.unica.co2.examples.insuredsale.InsuredSeller;
import it.unica.co2.examples.voucher.VoucherBuyer;
import it.unica.co2.examples.voucher.VoucherSeller;
import it.unica.co2.examples.voucher.VoucherService;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.honesty.HonestyResult;


public class HonestyTest {

	@Test
	public void simpleBuyer() {
		
		HonestyResult honesty = HonestyChecker.isHonest(SimpleBuyer.class);
		
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void multipleIfThenElse() {
		
		HonestyResult honesty = HonestyChecker.isHonest(MultipleIfThenElseProcess.class);
		
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void composition() {
		
		HonestyResult honesty = HonestyChecker.isHonest(ComposedProcess.class);
		
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void composition2() {
		
		HonestyResult honesty = HonestyChecker.isHonest(Composed2Process.class);
		
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	@Ignore	//too long
	public void parallel() {
		
		HonestyResult honesty = HonestyChecker.isHonest(ParallelProcess.class);
		
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void ebookstore() {
		
		HonestyResult honesty;
		
		honesty = HonestyChecker.isHonest(Buyer.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(Distributor.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(Seller.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(DishonestSeller.class);
		assertTrue(honesty==HonestyResult.DISHONEST);
	}
	
	@Test
	public void blackjack() {
		
		HonestyResult honesty;
		
		honesty = HonestyChecker.isHonest(Dealer.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(DeckService.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(Player.class);
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void voucher() {
		
		HonestyResult honesty;
		
		honesty = HonestyChecker.isHonest(VoucherService.class);
		assertTrue(honesty==HonestyResult.HONEST);

		honesty = HonestyChecker.isHonest(VoucherBuyer.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(VoucherSeller.class);
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
	@Test
	public void insuredSale() {
		
		HonestyResult honesty;
		
		honesty = HonestyChecker.isHonest(InsuredSeller.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(Insurance.class);
		assertTrue(honesty==HonestyResult.HONEST);
		
		honesty = HonestyChecker.isHonest(IBuyer.class);
		assertTrue(honesty==HonestyResult.HONEST);
	}
	
}
