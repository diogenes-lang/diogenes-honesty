package it.unica.co2.examples.mitm;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;

public class MasterAttacker extends Participant{

	private static final long serialVersionUID = 1L;

	private List<String> pairs = Collections.synchronizedList(new ArrayList<String>());
	
	protected MasterAttacker(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {

		ContractDefinition c = def("c").setContract(
				internalSum()
				.add("pair", Sort.string(),
						internalSum().add("range", Sort.string(),
								externalSum()
								.add("result", Sort.string())
								.add("abort")))
				.add("abort"));
		
		
		
		try {
			Public<TST> pblx = tell(c, 10_000);
			Session2<TST> x = waitForSession(pblx);

			try {
				Public<TST> pbly = tell(c, 10_000);
				Session2<TST> y = waitForSession(pbly);
				
				try {
					Public<TST> pblz = tell(c, 10_000);
					Session2<TST> z = waitForSession(pblz);
					//all session are fused
					
					processCall(Attack.class, pairs, x, y, z);
				}
				catch (ContractExpiredException e) {
					x.send("abort");
					y.send("abort");
				}
			}
			catch (ContractExpiredException e) {
				x.send("abort");
			}
		}
		catch (ContractExpiredException e) {

		}
	}
	
	public static class Attack extends CO2Process {

		private static final long serialVersionUID = 1L;

		private List<String> pairs;
		private final Session2<TST> x;
		private final Session2<TST> y;
		private final Session2<TST> z;

		public Attack(List<String> pairs, Session2<TST> x, Session2<TST> y, Session2<TST> z) {
			this.pairs = pairs;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public void run() {
			
			String key1 = AESUtils.getKey(1234);		//the key we want to crack (not known by the attacker)
			String key2 = AESUtils.getKey(42);			//the key we want to crack (not known by the attacker)
			String plaintext = "Hello world!";
			String ciphertext = AESUtils.encrypt(key2, AESUtils.encrypt(key1, plaintext));
			
			String pair = plaintext + "," + ciphertext;
			
			logger.log("pair to decrypt: "+pair);
			
			String rangex = "0-500";
			String rangey = "500-1000";
			String rangez = "1000-1500";

			Thread t1 = parallel(()->{
				processCall(CallToSlave.class, pairs, x, pair, rangex);
			});

			Thread t2 = parallel(()->{
				processCall(CallToSlave.class, pairs, y, pair, rangey);
			});

			Thread t3 = parallel(()->{
				processCall(CallToSlave.class, pairs, z, pair, rangez);
			});

			try {
				t1.join();
				t2.join();
				t3.join();
			}
			catch (InterruptedException e) {}
			
			logger.log("all slaves are terminated");
			
			Pair<String,String> keys = findKeys(pairs);
			checkKeys(plaintext, ciphertext, keys);
		}
		
		@SkipMethod
		private void checkKeys(String plaintext, String ciphertext, Pair<String, String> keys) {
			
			if (keys==null) {
				logger.log("key not found! Check your implementation");
			}
			else {
				logger.log("k1: "+ keys.getLeft());
				logger.log("k2: "+ keys.getRight());
				
				logger.log("plaintext: "+plaintext);
				logger.log("ciphertext: "+ciphertext);
				logger.log("plaintext (computed): "+AESUtils.decrypt(keys.getLeft(), AESUtils.decrypt(keys.getRight(), ciphertext)));
				logger.log("ciphertext (computed): "+AESUtils.encrypt(keys.getRight(), AESUtils.encrypt(keys.getLeft(), plaintext)));				
			}
		}
		
		@SkipMethod
		private Pair<String, String> findKeys(List<String> pairs) {
			logger.log("pairs.size(): "+pairs.size());
			
			Decoder decoder = Base64.getDecoder();
			
			Map<String, String> encPlaintextColl = new HashMap<>();
			Map<String, String> decCiphertextColl = new HashMap<>();
			
			for (String p: pairs) {
				p = new String(decoder.decode(p));
//				logger.log("p: "+p);
				
				for (String tripleS : p.split(",")) {
					String[] triple = tripleS.split("-");

					encPlaintextColl.put(triple[1], triple[0]);
					decCiphertextColl.put(triple[2], triple[0]);
				}
			}
			
			for (String encPlain : encPlaintextColl.keySet()) {
				
				if (decCiphertextColl.containsKey(encPlain)) {
					return new ImmutablePair<String, String>(
							encPlaintextColl.get(encPlain),
							decCiphertextColl.get(encPlain));
				}
			}
			
			return null;
		}
	}
	
	
	public static class CallToSlave extends CO2Process {

		private static final long serialVersionUID = 1L;

		private List<String> pairs;
		private final Session2<TST> x;
		private final String pair;
		private final String range;
		
		public CallToSlave(List<String> pairs, Session2<TST> x, String pair, String range) {
			super("Tid-"+Thread.currentThread().getId());
			this.pairs = pairs;
			this.x = x;
			this.pair = pair;
			this.range = range;
		}

		@Override
		public void run() {
			x.send("pair", pair);
			x.send("range", range);
			
			Message msg = x.waitForReceive("result", "abort");
			
			switch (msg.getLabel()) {
			case "result":
				try {
					String receivedPairs = msg.getStringValue();
					pairs.add(receivedPairs);
				}
				catch (ContractException e) {
					throw new RuntimeException(e);
				}
			}
			
		}
		
	}	
	
	public static void main(String[] args) {
		new MasterAttacker("mitm-master@nicola.com", "mitm-master").run();
	}
}