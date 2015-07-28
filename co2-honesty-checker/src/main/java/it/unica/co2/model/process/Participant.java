package it.unica.co2.model.process;

import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.util.ObjectUtils;
import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;


public abstract class Participant extends CO2Process {

	private static final long serialVersionUID = 1L;
	
	protected transient CO2ServerConnection connection;
	private final String username;
	private final String password;
	
	protected Participant(String username, String password) {
		super(username);
		this.username = username;
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
	
	protected Session2<TST> tellAndWait(Contract c) {
		try {
			return tellAndWait(c, -1);
		}
		catch (TimeExpiredException e) {
			/* you never go here */
			throw new RuntimeException(e);
		}
	}
	
	private Session2<TST> tellAndWait(Contract c, Integer timeout) throws TimeExpiredException {
		return waitForSession( tell(c) , timeout);
	}
	
	protected Public<TST> tell (Contract c) {
		
		if (connection==null)
			setConnection();
		
		try {
			serializedContract=ObjectUtils.serializeObjectToStringQuietly(c);
			sessionName = Session2.getNextSessionName();
			
			logger.log("middleware syntax contract <"+c.toMiddleware()+">");

			TST tst = new TST(c.toMiddleware());
			Private<TST> pvt = tst.toPrivate(connection);
			
			logger.log("telling contract <"+c+">");
			return pvt.tell();

		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Session2<TST> waitForSession(Public<TST> pbl) {
		try {
			return waitForSession(pbl, -1);
		}
		catch (TimeExpiredException e) {
			/* you never go here */
			throw new RuntimeException(e);
		}
	}
	
	protected Session2<TST> waitForSession(Public<TST> pbl, Integer timeout) throws TimeExpiredException {
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
	
	
	
	protected long parallel(CO2Process process) {
		logger.log("starting parallel process");
		Thread t = new Thread(process);
		t.start();
		return t.getId();
	}
}
