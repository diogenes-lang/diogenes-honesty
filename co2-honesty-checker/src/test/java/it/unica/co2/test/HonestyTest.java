package it.unica.co2.test;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.honesty.HonestyResult;
import it.unica.co2.test.processes.blackjack.Dealer;
import it.unica.co2.test.processes.blackjack.DeckService;
import it.unica.co2.test.processes.blackjack.Player;
import it.unica.co2.test.processes.ebookstore.Buyer;
import it.unica.co2.test.processes.ebookstore.DishonestSeller;
import it.unica.co2.test.processes.ebookstore.Distributor;
import it.unica.co2.test.processes.ebookstore.Seller;
import it.unica.co2.test.processes.insuredsale.IBuyer;
import it.unica.co2.test.processes.insuredsale.Insurance;
import it.unica.co2.test.processes.insuredsale.InsuredSeller;
import it.unica.co2.test.processes.travelagency.TravelAgency;
import it.unica.co2.test.processes.voucher.VoucherBuyer;
import it.unica.co2.test.processes.voucher.VoucherSeller;
import it.unica.co2.test.processes.voucher.VoucherService;


public class HonestyTest {

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
	@Ignore
	public void travelagency() {
		
		HonestyResult honesty;
		
		honesty = HonestyChecker.isHonest(TravelAgency.class);
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
