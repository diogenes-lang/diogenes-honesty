package it.unica.co2.examples.blackjack;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import static it.unica.co2.api.contract.newapi.ContractFactory.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.newapi.Recursion;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;


public class DeckService extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "bob@test.com";
	private static final String password = "bob";
	
	private List<Integer> deck = new ArrayList<>();
	
	public DeckService() {
		super(username, password);
		
		Integer[] cards = new Integer[]{1,2,3,4,5,6,7,8,9,10,10,10,10};
		
		deck.addAll(Arrays.asList(cards));
		deck.addAll(Arrays.asList(cards));
		deck.addAll(Arrays.asList(cards));
		deck.addAll(Arrays.asList(cards));
		
		assert deck.size()==52;
	}
	
	@Override
	public void run() {
		
		Recursion contract = recursion("x");
		contract.setContract(externalSum().add("next", internalSum().add("card", contract)).add("abort"));

		logger.log("tell and wait");
		Session2<TST> session = tellAndWait(contract);
		
		new Deck(session).run();
	}
	
	private class Deck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> session;
		
		protected Deck(Session2<TST> session) {
			super("Deck");
			this.session = session;
		}

		@Override
		public void run() {
			
			Message msg = session.waitForReceive("next", "abort");
			
			switch(msg.getLabel()) {
			case "next":
				session.send("card", getNextCard());
				new Deck(session).run();
				break;
				
			case "abort":
				break;
			}
			
		}
	}
	
	
	
	private int getNextCard() {
		
		int size = deck.size();
		
		assert size>0: "your deck is ended";
		
		int choice = new Random().nextInt(size);
		int card = deck.remove(choice);
		
		logger.log("extracted card "+card);
		
		assert deck.size()==size-1;
		
		return card;
	}
	
	public static void main(String args[]) {
		new DeckService().run();
	}
}
