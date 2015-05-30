package it.unica.co2.model;


public class SumOperand {

	private final Prefix prefix;
	private final Process process;
	
	public SumOperand(Prefix prefix) {
		this(prefix, null);
	}

	public SumOperand(Prefix prefix, Process process) {
		super();
		this.prefix = prefix;
		this.process = process;
	}

	public Prefix getPrefix() {
		return prefix;
	}
	
	public Process getProcess() {
		return process;
	}
	
	@Override
	public String toString() {
		return prefix+".<process>";
	}
}
