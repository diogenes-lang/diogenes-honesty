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


public abstract class Participant extends CO2Process {

	private static final long serialVersionUID = 1L;
	
	protected CO2ServerConnection connection;

	protected Participant(String username, String password) {
		super(username);
		
		try {
			logger.log("creating new connection: username=<"+username+"> password=<"+password+">");
			connection = new CO2ServerConnection(username, password);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	abstract protected String getUsername();
	
	
	/*
	 * JPF-fields
	 */
	@SuppressWarnings("unused") private String serializedContract;
	@SuppressWarnings("unused") private String sessionName;
	
	protected Session2<TST> tellAndWait(Contract c) {
		return tellAndWait(c, -1);
	}
	
	private Session2<TST> tellAndWait(Contract c, Integer timeout) {
		return waitForSession( tell(c) , timeout);
	}
	
	protected Public<TST> tell (Contract c) {
		try {
			serializedContract=ObjectUtils.serializeObjectToStringQuietly(c);
			sessionName = Session2.getNextSessionName();
			
			TST tstA = new TST(c.toMiddleware());
			Private<TST> pvtA = tstA.toPrivate(connection);
			
			logger.log("telling contract <"+c+">");
			return pvtA.tell();

		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Session2<TST> waitForSession(Public<TST> pbl) {
		return waitForSession(pbl, -1);
	}
	
	protected Session2<TST> waitForSession(Public<TST> pbl, Integer timeout) {
		try {
			
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
	
	
	
	protected void startNewMacro(String name) {}
}
