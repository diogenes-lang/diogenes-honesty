package it.unica.co2.api.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.util.ObjectUtils;



public abstract class Participant extends CO2Process {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(Participant.class);
	
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
	
	
	
	public Session<TST> tellAndWait(Contract c) {
		return tellAndWait(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c));
	}
	
	public Session<TST> tellAndWait(ContractDefinition c) {
		return tell(c).waitForSession();
	}

	public Session<TST> tellAndWait(Contract c, int timeout) throws ContractExpiredException {
		return tellAndWait(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c), timeout);
	}
	
	public Session<TST> tellAndWait(ContractDefinition c, int timeout) throws ContractExpiredException {
		return tell(c, timeout).waitForSession();
	}

	public Public<TST> tell (Contract c) {
		return tell(c, 0);
	}
	
	public Public<TST> tell (ContractDefinition cDef) {
		return tell(cDef, 0);
	}

	public Public<TST> tell (Contract c, Integer delay) {
		return tell(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c), delay);
	}
	
	public Public<TST> tell (ContractDefinition cDef, Integer delay) {
		if (connection==null)
			setConnection();

		String cserial = ObjectUtils.serializeObjectToStringQuietly(cDef);
		
		try {
			String context = cDef.getContract().getContext();
			TST tst = new TST(cDef.getContract().toTST());
			
			if (context!=null && !context.trim().isEmpty()) {
				tst.setContext(context);
			}
			
			Private<TST> pvt = tst.toPrivate(connection);
			return _tell(cDef, cserial, pvt, delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Public<TST> _tell (ContractDefinition cDef, String cserial, Private<TST> pvt, Integer delay) {
		
		try {
			logger.info("telling contract <{}> with delay {} msec", cDef.getContract().toTST(), delay);
			return pvt.tell(delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
}
