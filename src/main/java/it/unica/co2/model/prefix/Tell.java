package it.unica.co2.model.prefix;

import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.util.ObjectUtils;
import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;

public class Tell extends Prefix {

	private final CO2ServerConnection connection;
	private final Contract contract;
	private Session2<TST> session;
	
	/* JPF-specific fields */
	@SuppressWarnings("unused") private String serializedContract;
	@SuppressWarnings("unused") private String sessionName;
	
	public Tell(String username, CO2ServerConnection connection, Contract contract) {
		super(username);
		this.connection = connection;
		this.contract = contract;

		this.serializedContract = ObjectUtils.serializeObjectToStringQuietly(contract);
	}

	@Override
	public void run() {

		try {
			
			TST tstA = new TST(contract.toMiddleware());
			Private<TST> pvtA = tstA.toPrivate(connection);
			
			logger.log("telling contract <"+contract+">");
			Public<TST> pbl = pvtA.tell();
			
			logger.log("waiting for session");
			Session<TST> session = pbl.waitForSession();
			
			this.session = new Session2<TST>(connection, session.getContract());	//wrap to the session do get an unique name
			this.sessionName = this.session.getSessionName();
			
			logger.log("session fused");
		}
		catch(ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Session2<TST> getSession() {
		return session;
	}

	@Override
	public String toString() {
		return "tell ("+contract+")";
	}

}
