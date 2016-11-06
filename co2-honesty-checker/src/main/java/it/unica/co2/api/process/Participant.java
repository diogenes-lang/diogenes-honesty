package it.unica.co2.api.process;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.util.ObjectUtils;



public abstract class Participant extends CO2Process {

	private static final long serialVersionUID = 1L;
	
	private transient CO2ServerConnection connection;
	protected final String username;
	protected final String password;
	
	protected Participant(String username, String password) {
		this.username = username;
		this.password = password;
	}

	
	private void setConnection() {
		try {
			logger.info("creating new connection: username=<"+username+"> password=<"+password+">");
			connection = new CO2ServerConnection(username, password);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	public Session<SessionType> tellAndWait(SessionType c) {
		return tellAndWait(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c));
	}
	
	public Session<SessionType> tellAndWait(ContractDefinition c) {
		return tell(c).waitForSession();
	}

	public Session<SessionType> tellAndWait(SessionType c, int timeout) throws ContractExpiredException {
		return tellAndWait(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c), timeout);
	}
	
	public Session<SessionType> tellAndWait(ContractDefinition c, int timeout) throws ContractExpiredException {
		return tell(c, timeout).waitForSession();
	}

	public Public<SessionType> tell (SessionType c) {
		return tell(c, 0);
	}
	
	public Public<SessionType> tell (ContractDefinition cDef) {
		return tell(cDef, 0);
	}

	public Public<SessionType> tell (SessionType c, Integer delay) {
		return tell(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c), delay);
	}
	
	public Public<SessionType> tell (ContractDefinition cDef, Integer delay) {
		if (connection==null)
			setConnection();

		String cserial = ObjectUtils.serializeObjectToStringQuietly(cDef);
		
		try {
			Private<SessionType> pvt = cDef.getContract().toPrivate(connection);
			return _tell(cDef, cserial, pvt, delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Public<SessionType> _tell (ContractDefinition cDef, String cserial, Private<SessionType> pvt, Integer delay) {
		
		try {
			logger.info("telling contract <{}> with delay {} msec", cDef.getContract().toTST(), delay);
			return pvt.tell(delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
}
