package it.unica.co2.compliance.lts;

import java.io.Serializable;

public interface LTSTransition extends Serializable {

	public LTSState apply();
}
