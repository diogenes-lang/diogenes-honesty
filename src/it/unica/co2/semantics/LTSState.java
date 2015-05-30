package it.unica.co2.semantics;

public interface LTSState<T> {

	public boolean hasNext();
	
	public LTSState<T>[] nextStates();
	
}
