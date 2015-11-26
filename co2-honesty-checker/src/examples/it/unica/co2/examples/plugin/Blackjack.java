package it.unica.co2.examples.plugin;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
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
			super(username, password);
		}
		
		@Override
		public void run() {
			Public<TST> pbl$xd$Cd = tell(Cd.getContract());
			Session2<TST> xd = waitForSession(pbl$xd$Cd);
			
			Public<TST> pbl$xp$Cp = tell(Cp.getContract());
			
			try {
				Session2<TST> xp = waitForSession(pbl$xp$Cp, 10000);
				new Pplay(xp,xd,0).run();
			}
			catch(TimeExpiredException e) {
				xd.send("abort");
				Session2<TST> xp$0 = waitForSession(pbl$xp$Cp);
				new PabortP(xp$0).run();
			}
		}
	}
	
	public static class Pplay extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		
		public Pplay(Session2<TST> xp,Session2<TST> xd,Integer np) {
			super(username, password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
		}
		
		@Override
		public void run() {
			try {
				logger.log("waiting on 'xp' for actions [hit,stand]");
				Message msg$0 = xp.waitForReceive(10000, "hit","stand");
				
				switch (msg$0.getLabel()) {			
					
					case "hit":
						logger.log("received [hit]");
						xd.send("next");
						new Pdeck(xp,xd,np).run();
						break;
					
					case "stand":
						logger.log("received [stand]");
						new Qstand(xp,xd,np,0).run();
						break;
					
					default:
						throw new IllegalStateException("You should not be here");
				}
			}
			catch (TimeExpiredException e) {
				xd.send("abort");
				new PabortP(xp).run();
			}
			
		}
	}
	
	public static class Pdeck extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		
		public Pdeck(Session2<TST> xp,Session2<TST> xd,Integer np) {
			super(username, password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
		}
		
		@Override
		public void run() {
			try {
				logger.log("waiting on 'xd' for actions [card]");
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
				xp.send("abort");
				new PabortD(xd).run();
			}
			
		}
	}
	
	public static class Pcard extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		private Integer n;
		
		public Pcard(Session2<TST> xp,Session2<TST> xd,Integer np,Integer n) {
			super(username, password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.n=n;
		}
		
		@Override
		public void run() {
			if ((np<21)) {
				xp.send("card", n);
				new Pplay(xp,xd,np).run();
			}
			else {
				xp.send("lose");
				new PabortD(xd).run();
			}
		}
	}
	
	public static class Qstand extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qstand(Session2<TST> xp,Session2<TST> xd,Integer np,Integer nd) {
			super(username, password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.nd=nd;
		}
		
		@Override
		public void run() {
			if ((nd<21)) {
				xd.send("next");
				new Qdeck(xp,xd,np,nd).run();
			}
			else {
				xp.send("win");
				xd.send("abort");
			}
		}
	}
	
	public static class Qdeck extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qdeck(Session2<TST> xp,Session2<TST> xd,Integer np,Integer nd) {
			super(username, password);
			this.xp=xp;
			this.xd=xd;
			this.np=np;
			this.nd=nd;
		}
		
		@Override
		public void run() {
			try {
				logger.log("waiting on 'xd' for actions [card]");
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
				xp.send("abort");
				new PabortD(xd).run();
			}
			
		}
	}
	
	public static class Qcard extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		private Session2<TST> xd;
		private Integer np;
		private Integer nd;
		
		public Qcard(Session2<TST> xp,Session2<TST> xd,Integer np,Integer nd) {
			super(username, password);
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
				xp.send("lose");
				new PabortD(xd).run();
			}
		}
	}
	
	public static class PabortP extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xp;
		
		public PabortP(Session2<TST> xp) {
			super(username, password);
			this.xp=xp;
		}
		
		@Override
		public void run() {
			logger.log("waiting on 'xp' for actions [hit,stand]");
			Message msg$0 = xp.waitForReceive("hit","stand");
			
			switch (msg$0.getLabel()) {			
				
				case "hit":
					logger.log("received [hit]");
					xp.send("abort");
					break;
				
				case "stand":
					logger.log("received [stand]");
					xp.send("abort");
					break;
				
				default:
					throw new IllegalStateException("You should not be here");
			}
		}
	}
	
	public static class PabortD extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session2<TST> xd;
		
		public PabortD(Session2<TST> xd) {
			super(username, password);
			this.xd=xd;
		}
		
		@Override
		public void run() {
			
			parallel(()->{
				xd.send("abort");
			});
			
			parallel(()->{
				logger.log("waiting on 'xd' for actions [card]");
				Message msg$0 = xd.waitForReceive("card");
				
				Integer n$card$msg1;
				try {
					n$card$msg1 = Integer.parseInt(msg$0.getStringValue());
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				xd.send("abort");
			});
		}
	}
	
	public static void main(String[] args) {
		new P().run();
	}
}
