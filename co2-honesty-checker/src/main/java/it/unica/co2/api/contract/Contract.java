package it.unica.co2.api.contract;

import java.io.Serializable;

import it.unica.co2.api.contract.bekic.Bekic;
import it.unica.co2.api.contract.generators.MaudeContractGenerator;
import it.unica.co2.api.contract.generators.TSTContractGenerator;

public abstract class Contract implements Serializable {

	private static final long serialVersionUID = 1L;

	private Action preceeding;
	
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
		
		return new TSTContractGenerator(
				Bekic.getInstance(this).defToRec()
				).generate();
	}
}
