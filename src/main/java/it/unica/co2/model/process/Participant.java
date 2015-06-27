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


public abstract class Participant extends Process {

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
	
	protected Session2<TST> tell(Contract c) {
		try {
			serializedContract=ObjectUtils.serializeObjectToStringQuietly(c);
			
			TST tstA = new TST(c.toMiddleware());
			Private<TST> pvtA = tstA.toPrivate(connection);
			
			logger.log("telling contract <"+c+">");
			Public<TST> pbl = pvtA.tell();
			
			logger.log("waiting for session");
			Session<TST> session = pbl.waitForSession();
			
			Session2<TST> session2 = new Session2<TST>(connection, session.getContract());	//wrap to the session2
			logger.log("session fused");
			
			sessionName = session2.getSessionName();
			
			return session2;
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
}
