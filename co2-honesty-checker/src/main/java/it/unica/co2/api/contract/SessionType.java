package it.unica.co2.api.contract;

import java.io.Serializable;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractModel;
import co2api.ContractXML;
import co2api.Private;
import co2api.TST;
import it.unica.co2.api.contract.bekic.Bekic;
import it.unica.co2.api.contract.generators.MaudeContractGenerator;
import it.unica.co2.api.contract.generators.TSTContractGenerator;

public abstract class SessionType implements Serializable, ContractModel {

	private static final long serialVersionUID = 1L;

	private String context;
	private Action preceeding;
	
	public String getContext() {
		return context;
	}
	
	public SessionType setContext(String context) {
		this.context = context;
		return this;
	}
	
	public Action getPreceeding() {
		return preceeding;
	}

	public void setPreceeding(Action preceeding) {
		this.preceeding = preceeding;
	}
	
	public abstract SessionType deepCopy();
	
	public String toMaude() {
		return new MaudeContractGenerator(this).generate();
	}
	
	public String toTST() {
		
		Bekic instance = Bekic.getInstance(this);
		SessionType c = instance.defToRec();
		
		return new TSTContractGenerator(c).generate();
	}

	@Override
	public ContractXML getXML() {
		if (context!=null)
			return new TST(toTST(), context).getXML();
		else
			return new TST(toTST()).getXML();
	}

	@Override
	public void setFromString(String arg0) throws ContractException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFromXML(ContractXML arg0) throws ContractException {
		throw new UnsupportedOperationException();	
	}

	@SuppressWarnings("unchecked")
	@Override
	public Private<SessionType> toPrivate(CO2ServerConnection conn) {
		return new Private<SessionType>(conn, this);
	}
	
}
