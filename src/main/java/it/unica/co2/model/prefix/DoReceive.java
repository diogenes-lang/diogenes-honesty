package it.unica.co2.model.prefix;


import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Sort;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class DoReceive extends Prefix {

	private final Session2<TST> session;
	private final Action action;
	private final Variable receivedValue;

	public DoReceive(String username, Session2<TST> session, Variable variable, Action action) {
		super(username);
		this.session = session;
		this.receivedValue = variable;
		this.action = action;
	}
	
	@Override
	public void run() {
		
		logger.log("allowed input: "+ action);
		
		try {
			if (session!=null) {
				
				logger.log("waiting for a message");
				Message msg = session.waitForReceive();
			
				if (msg!=null) {

					String label = msg.getLabel();
					
					if (action.getName().equals(label)) {
						
						switch (action.getSort()) {
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
							throw new AssertionError("unexpected sort "+action.getSort());
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

}
