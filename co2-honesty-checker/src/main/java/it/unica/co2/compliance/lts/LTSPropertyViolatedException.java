package it.unica.co2.compliance.lts;

import it.unica.co2.util.ObjectUtils;

import java.util.List;

@SuppressWarnings("unused")
public class LTSPropertyViolatedException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private String finalState;	//the state that violate the property
	private String path;
	
	public LTSPropertyViolatedException(LTSState state, List<LTSState> path) {
		this.finalState=ObjectUtils.serializeObjectToStringQuietly(state);
		this.path=ObjectUtils.serializeObjectToStringQuietly(path);
	}

}
