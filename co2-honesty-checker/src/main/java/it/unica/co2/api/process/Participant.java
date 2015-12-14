package it.unica.co2.api.process;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;
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
	
	
	
	public Session<TST> tellAndWait(Contract c) {
		return tellAndWait(new ContractDefinition("anon"+System.currentTimeMillis()).setContract(c));
	}
	
	public Session<TST> tellAndWait(ContractDefinition c) {
		return tell(c).waitForSession();
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
			TST tst = new TST(cDef.getContract().toTST());
			Private<TST> pvt = tst.toPrivate(connection);
			return _tell(cDef, cserial, pvt, delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Public<TST> _tell (ContractDefinition cDef, String cserial, Private<TST> pvt, Integer delay) {
		
		try {
			logger.log("telling contract <"+cDef.getContract().toTST()+">");
			return pvt.tell(delay);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
}
