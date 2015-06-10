package it.unica.co2.semantics;

import java.io.Serializable;

public interface LTSTransition extends Serializable {

	public LTSState apply();
}
