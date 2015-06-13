package it.unica.co2.semantics.lts;

import it.unica.co2.util.ObjectUtils;

import java.io.IOException;
import java.util.List;

public class LTSPropertyViolatedException extends RuntimeException {

	private String finalState;	//the state that violate the property
	private String path;
	
	public LTSPropertyViolatedException(LTSState state, List<LTSState> path) {
		try {
			this.finalState=ObjectUtils.serializeObjectToString(state);
			this.path=ObjectUtils.serializeObjectToString(path);
		} catch (IOException e) {
			e.printStackTrace();
			this.finalState="an error occur serializing the object";
			this.path="an error occur serializing the object";
		}
	}

	public String getFinalState() {
		return finalState;
	}

	public String getPath() {
		return path;
	}

}
