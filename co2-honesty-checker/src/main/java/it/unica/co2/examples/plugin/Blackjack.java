package it.unica.co2.examples.plugin;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;

/*
 * auto-generated by co2-plugin
 * creation date: 04-09-2015 11:16:44
 */

@SuppressWarnings("unused")
public class Blackjack {
	
	private static String username = "test@co2-plugin.com";
	private static String password = "test";
	
	
	/*
	 * contracts declaration
	 */
	private static ContractDefinition Cp = def("Cp");
	private static Recursion rec_Cp_Z_0 = recursion("x");
	private static ContractDefinition Cd = def("Cd");
	private static Recursion rec_Cd_Z_0 = recursion("x");
	
	/*
	 * contracts initialization
	 */
	static {
		Cp.setContract(rec_Cp_Z_0);
		rec_Cp_Z_0.setContract(externalSum().add("hit", Sort.unit(), internalSum().add("card", Sort.integer(), rec_Cp_Z_0).add("lose", Sort.unit()).add("abort", Sort.unit())).add("stand", Sort.unit(), internalSum().add("win", Sort.unit()).add("lose", Sort.unit()).add("abort", Sort.unit())));
		Cd.setContract(rec_Cd_Z_0);
		rec_Cd_Z_0.setContract(internalSum().add("next", Sort.unit(), externalSum().add("card", Sort.integer(), rec_Cd_Z_0)).add("abort", Sort.unit()));
	}
	
	public static class P extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		public P() {
			super(Blackjack.username, Blackjack.password);
		}
		
		@Override
		public void run() {
			Public<TST> pbl$xd$Cd = tell(Cd.getContract());
			Session<TST> xd = pbl$xd$Cd.waitForSession();
			
			Public<TST> pbl$xp$Cp = tell(Cp.getContract());
			
			try {
				Session<TST> xp = pbl$xp$Cp.waitForSession(10000);
				new Pplay(xp,xd,0).run();
			}
			catch(TimeExpiredException e) {
				xd.sendIfAllowed("abort");
				Session<TST> xp$0 = pbl$xp$Cp.waitForSession();
				new PabortP(xp$0).run();
			}
		}
	}
	
	public static class Pplay extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		
		public Pplay(Session<TST> xp,Session<TST> xd,Integer np) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("waiting on 'xp' for actions [hit,stand]");
				Message msg$0 = xp.waitForReceive(10000, "hit","stand");
				
				switch (msg$0.getLabel()) {			
					
					case "hit":
					System.out.println("received [hit]");
						xd.sendIfAllowed("next");
						new Pdeck(xp,xd,np).run();
						break;
					
					case "stand":
					System.out.println("received [stand]");
						new Qstand(xp,xd,np,0).run();
						break;
					
					default:
						throw new IllegalStateException("You should not be here");
				}
			}
			catch (TimeExpiredException e) {
				xd.sendIfAllowed("abort");
				new PabortP(xp).run();
			}
			
		}
	}
	
	public static class Pdeck extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		
		public Pdeck(Session<TST> xp,Session<TST> xd,Integer np) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("waiting on 'xd' for actions [card]");
				Message msg$0 = xd.waitForReceive(10000, "card");
				
				Integer n$card$msg1;
				try {
					n$card$msg1 = Integer.parseInt(msg$0.getStringValue());
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				new Pcard(xp,xd,(np+n$card$msg1),n$card$msg1).run();
			}
			catch (TimeExpiredException e) {
				xp.sendIfAllowed("abort");
				new PabortD(xd).run();
			}
			
		}
	}
	
	public static class Pcard extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		private Integer n;
		
		public Pcard(Session<TST> xp,Session<TST> xd,Integer np,Integer n) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.n=n;
		}
		
		@Override
		public void run() {
			if ((np<21)) {
				xp.sendIfAllowed("card", n);
				new Pplay(xp,xd,np).run();
			}
			else {
				xp.sendIfAllowed("lose");
				new PabortD(xd).run();
			}
		}
	}
	
	public static class Qstand extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qstand(Session<TST> xp,Session<TST> xd,Integer np,Integer nd) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.nd=nd;
		}
		
		@Override
		public void run() {
			if ((nd<21)) {
				xd.sendIfAllowed("next");
				new Qdeck(xp,xd,np,nd).run();
			}
			else {
				xp.sendIfAllowed("win");
				xd.sendIfAllowed("abort");
			}
		}
	}
	
	public static class Qdeck extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qdeck(Session<TST> xp,Session<TST> xd,Integer np,Integer nd) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.nd=nd;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("waiting on 'xd' for actions [card]");
				Message msg$0 = xd.waitForReceive(10000, "card");
				
				Integer n$card$msg1;
				try {
					n$card$msg1 = Integer.parseInt(msg$0.getStringValue());
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				new Qcard(xp,xd,np,nd).run();
			}
			catch (TimeExpiredException e) {
				xp.sendIfAllowed("abort");
				new PabortD(xd).run();
			}
			
		}
	}
	
	public static class Qcard extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		private Session<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qcard(Session<TST> xp,Session<TST> xd,Integer np,Integer nd) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.nd=nd;
		}
		
		@Override
		public void run() {
			if ((nd<np)) {
				new Qstand(xp,xd,np,nd).run();
			}
			else {
				xp.sendIfAllowed("lose");
				new PabortD(xd).run();
			}
		}
	}
	
	public static class PabortP extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xp;
		
		public PabortP(Session<TST> xp) {
			super(Blackjack.username, Blackjack.password);
			this.xp=xp;
		}
		
		@Override
		public void run() {
			System.out.println("waiting on 'xp' for actions [hit,stand]");
			Message msg$0 = xp.waitForReceive("hit","stand");
			
			switch (msg$0.getLabel()) {			
				
				case "hit":
				System.out.println("received [hit]");
					xp.sendIfAllowed("abort");
					break;
				
				case "stand":
				System.out.println("received [stand]");
					xp.sendIfAllowed("abort");
					break;
				
				default:
					throw new IllegalStateException("You should not be here");
			}
		}
	}
	
	public static class PabortD extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> xd;
		
		public PabortD(Session<TST> xd) {
			super(Blackjack.username, Blackjack.password);
			this.xd=xd;
		}
		
		@Override
		public void run() {
			
			parallel(()->{
				xd.sendIfAllowed("abort");
			});
			
			parallel(()->{
				System.out.println("waiting on 'xd' for actions [card]");
				Message msg$0 = xd.waitForReceive("card");
				
				Integer n$card$msg1;
				try {
					n$card$msg1 = Integer.parseInt(msg$0.getStringValue());
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				xd.sendIfAllowed("abort");
			});
		}
	}
	
	public static void main(String[] args) {
		new P().run();
	}
}