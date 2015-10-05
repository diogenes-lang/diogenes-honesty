package it.unica.co2.api.process;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.util.ObjectUtils;



public abstract class Participant extends CO2Process {

	private static final long serialVersionUID = 1L;
	
	protected Participant(String username, String password) {
		super(username);
		this.setUsername(username);
		this.setPassword(password);
	}
	
	private transient CO2ServerConnection connection;
	private String username;
	private String password;
		

	
	private void setUsername(String username) {
		this.username = username;
	}

	private void setPassword(String password) {
		this.password = password;
	}

	private void setConnection() {
		try {
			logger.log("creating new connection: username=<"+username+"> password=<"+password+">");
			connection = new CO2ServerConnection(username, password);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	/*
	 * JPF-fields
	 */
	@SuppressWarnings("unused") private String serializedContract;
	@SuppressWarnings("unused") private String sessionName;
	
	
	public Session2<TST> tellAndWait(ContractDefinition cDef) {
		return tellAndWait(cDef.getContract());
	}
	
	public Session2<TST> tellAndWait(Contract c) {
		try {
			return tellAndWait(c, -1);
		}
		catch (TimeExpiredException e) {
			/* you never go here */
			throw new RuntimeException(e);
		}
	}
	
	//keep it private!
	private Session2<TST> tellAndWait(Contract c, Integer timeout) throws TimeExpiredException {
		return waitForSession( tell(c) , timeout);
	}
	
	
	
	public Public<TST> tell (ContractDefinition cDef) {
		return tell(cDef.getContract());
	}
	
	public Public<TST> tell (ContractDefinition cDef, Integer delay) {
		return tell(cDef.getContract(), delay);
	}
	
	public Public<TST> tell (Contract c) {
		return tell(c, 0);
	}
	
	public Public<TST> tell (Contract c, Integer delay) {
		
		if (connection==null)
			setConnection();

		try {
			serializedContract=ObjectUtils.serializeObjectToStringQuietly(c);
			sessionName = Session2.getNextSessionName();
			
			logger.log("middleware syntax contract <"+c.toTST()+">");

			TST tst = new TST(c.toTST());
			Private<TST> pvt = tst.toPrivate(connection);
			
			logger.log("telling contract <"+c+">");
			return pvt.tell();

		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Session2<TST> waitForSession(Public<TST> pbl) {
		try {
			return waitForSession(pbl, -1);
		}
		catch (TimeExpiredException e) {
			/* you never go here */
			throw new RuntimeException(e);
		}
	}
	
	public Session2<TST> waitForSession(Public<TST> pbl, Integer timeout) throws TimeExpiredException {
		try {
			logger.log("waiting for a session");
			
			Session<TST> session = null;
			if (timeout==-1) {
				session = pbl.waitForSession();
			}
			else {
				session = pbl.waitForSession(timeout);
			}
			
			logger.log("session fused");
			return new Session2<TST>(connection, session.getContract());
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
}
