package it.unica.co2.compliance.lts;

import java.io.Serializable;

public interface LTSState extends Serializable {

	public boolean hasNext();
	
	public LTSTransition[] getAvailableTransitions();
	
	public LTSTransition getPrecededTransition();
	
	public void setPrecedingTransition(LTSTransition transition);
	
	public boolean check();
}
