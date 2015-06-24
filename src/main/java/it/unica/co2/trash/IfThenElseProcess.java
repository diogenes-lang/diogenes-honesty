package it.unica.co2.trash;

import it.unica.co2.model.process.Process;

import java.util.function.Supplier;

public class IfThenElseProcess extends Process {

	private final Process thenProcess;
	private final Process elseProcess;
	private final Supplier<Boolean> condition;
	

	public IfThenElseProcess(String username, Process thenProcess, Process elseProcess, Supplier<Boolean> condition) {
		super(username);
		this.thenProcess = thenProcess;
		this.elseProcess = elseProcess;
		this.condition = condition;
	}

	@Override
	public void run() {
		System.out.println("IfThenElse process");
		
		if (condition.get()) {
			System.out.println("choosed THEN branch");
			thenProcess.run();
		}
		else {
			System.out.println("choosed ELSE branch");
			elseProcess.run();
		}
	}

	public Supplier<Boolean> getCondition() {
		return condition;
	}

}
