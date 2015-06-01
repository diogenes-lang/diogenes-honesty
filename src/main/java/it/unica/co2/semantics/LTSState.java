package it.unica.co2.semantics;

public interface LTSState {

	public boolean hasNext();
	
	public LTSState[] nextStates();
	
	public boolean check();
	
}
