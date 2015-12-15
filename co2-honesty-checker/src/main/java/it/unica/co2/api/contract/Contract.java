package it.unica.co2.api.contract;

import java.io.Serializable;

import it.unica.co2.api.contract.bekic.Bekic;
import it.unica.co2.api.contract.generators.MaudeContractGenerator;
import it.unica.co2.api.contract.generators.TSTContractGenerator;

public abstract class Contract implements Serializable {

	private static final long serialVersionUID = 1L;

	private String context;
	private Action preceeding;
	
	public String getContext() {
		return context;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public Action getPreceeding() {
		return preceeding;
	}

	public void setPreceeding(Action preceeding) {
		this.preceeding = preceeding;
	}
	
	public abstract Contract deepCopy();
	
	public String toMaude() {
		return new MaudeContractGenerator(this).generate();
	}
	
	public String toTST() {
		
		Bekic instance = Bekic.getInstance(this);
		Contract c = instance.defToRec();
		
		return new TSTContractGenerator(c).generate();
	}

}
