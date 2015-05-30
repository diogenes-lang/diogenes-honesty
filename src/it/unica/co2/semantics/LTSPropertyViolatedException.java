package it.unica.co2.semantics;

public class LTSPropertyViolatedException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private LTSState state;		//the state that violate the property
	
	public LTSPropertyViolatedException() {
		super();
	}

	public LTSPropertyViolatedException(String message, Throwable cause) {
		super(message, cause);
	}

	public LTSPropertyViolatedException(String message) {
		super(message);
	}

	public LTSPropertyViolatedException(Throwable cause) {
		super(cause);
	}

	public LTSPropertyViolatedException(LTSState state) {
		this.state=state;
	}

	public LTSState getState() {
		return state;
	}

}
