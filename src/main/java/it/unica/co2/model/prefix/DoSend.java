package it.unica.co2.model.prefix;

import it.unica.co2.api.Session2;
import co2api.TST;


public class DoSend extends Prefix {

	private Session2<TST> session;
	private Action action;
	private final String value;
	
	/* JPF-specific fields */
	@SuppressWarnings("unused") private String sessionName;
	@SuppressWarnings("unused") private String actionName;
	
	public DoSend(String username, Session2<TST> session, Action action) {
		this(username, session, action, null);
	}
	
	public DoSend(String username, Session2<TST> session, Action action, String value) {
		super(username);
		this.session = session;
		this.action = action;
		this.value=value;
		
		this.sessionName = session.getSessionName();
		this.actionName = action.getName();	
	}

	@Override
	public void run() {
		
		logger.log("sending action "+action.getName()+"! "+(value!=null? value:""));

		if (session!=null) {
			switch (action.getSort()) {
			
			case INT:
				assert value!=null;
				session.send(action.getName(), value);
				break;
				
			case STRING:
				assert value!=null;
				session.send(action.getName(), value);
				break;
				
			case UNIT:
				session.send(action.getName());
				break;
			default:
				throw new RuntimeException();
			}
			
			if (action.getNext()!=null) {
				logger.log("executing next");
				action.getNext().run();
			}
		}
	}
	
	@Override
	public String toString() {
		return "do "+session+" "+action.getName()+"! "+(value!=null? value:"");
	}
}
