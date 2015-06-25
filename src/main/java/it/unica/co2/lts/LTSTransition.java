package it.unica.co2.lts;

import java.io.Serializable;

public interface LTSTransition extends Serializable {

	public LTSState apply();
}
