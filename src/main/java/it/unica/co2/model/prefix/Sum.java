package it.unica.co2.model.prefix;

import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import co2api.ContractException;
import co2api.Message;
import co2api.TST;


public class Sum extends Process {

	private static final Random rand = new Random();
	
	private final List<DoSend> doSends = new ArrayList<>();
	private final List<Action> doReceives = new ArrayList<>();

	private final Session2<TST> session;
	private final Variable receivedValue;
	
	public Sum(String username, Session2<TST> session, Variable variable) {
		super(username);
		this.session = session;
		this.receivedValue = variable;
	}
	
	

	public Sum add(DoSend prefix) {
		doSends.add(prefix);
		return this;
	}
	
	public Sum add(Action prefix) {
		doReceives.add(prefix);
		return this;
	}
	
	@Override
	public void run() {
		
		if (doSends.size()>0 && doReceives.size()>0 ) {
			throw new AssertionError("you can't sum both DoSend and DoReceive");
		}
		
		if (doSends.size()>0) {
			sumSend();
		}
		else if (doReceives.size()>0) {
			sumReceive();
		}
		else {
			logger.log("nothing to do");
		}
	}
	
	private void sumSend(){
		logger.log("sum of send: choosing randomly");
		int choice = rand.nextInt(doSends.size());
		
		DoSend send = doSends.get(choice);
		logger.log("choosed prefix:"+ send);
		
		send.run();
	}
	
	private void sumReceive(){
		logger.log("sum of receive");
		
		logger.log("allowed input: "+ doReceives);
		
		try {
			if (session!=null) {
				
				logger.log("waiting for a message");
				Message msg = session.waitForReceive();
			
				if (msg!=null) {

					String label = msg.getLabel();
					
					
					for (Action a : doReceives) {
						
						if (a.getName().equals(label)) {
							
							switch (a.getSort()) {
							case INT:
								assert receivedValue!=null;
								assert receivedValue.getSort()==Sort.INT;
								receivedValue.setMsg(new Message(msg.getLabel(), Integer.valueOf(msg.getStringValue())));
								break;

							case STRING:
								assert receivedValue!=null;
								assert receivedValue.getSort()==Sort.STRING;
								receivedValue.setMsg(msg);
								break;

							case UNIT:
								assert receivedValue!=null;
								assert receivedValue.getSort()==Sort.UNIT;
								receivedValue.setMsg(msg);
								break;
								
							default:
								throw new AssertionError("unexpected sort "+a.getSort());
							}
							
							if (a.getNext()!=null) {
								logger.log("executing next");
								a.getNext().run();
							}
							
							break;
						}
					}
					
					logger.log("received message: "+ msg.getLabel()+" "+msg.getStringValue());
					
					if (receivedValue==null)
						throw new AssertionError("received unexpected message");
					
				}
					
			}
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}



	@Override
	public String toString() {
		return "Sum [doSends=" + doSends + ", doReceives=" + doReceives + "]";
	}
	
}
