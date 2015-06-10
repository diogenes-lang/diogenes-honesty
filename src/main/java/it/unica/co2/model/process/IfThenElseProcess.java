package it.unica.co2.model.process;

import java.util.Random;
import java.util.function.Supplier;

public class IfThenElseProcess extends Process {

	private final Process thenProcess;
	private final Process elseProcess;
	private Supplier<Boolean> condition = new RandomSupplier();
	
	public IfThenElseProcess(Process thenProcess, Process elseProcess) {
		this(thenProcess, elseProcess, new RandomSupplier());
	}

	public IfThenElseProcess(Process thenProcess, Process elseProcess, Supplier<Boolean> condition) {
		this.thenProcess = thenProcess;
		this.elseProcess = elseProcess;
		this.condition = condition;
	}

	@Override
	public void run() {
		System.out.println("IfThenElse process");
		System.out.println("is choosing randomly: "+isChoosingRandomly());
		
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

	public void setCondition(Supplier<Boolean> condition) {
		this.condition = condition;
	}

	
	
	private static final class RandomSupplier implements Supplier<Boolean> {
		
		private final Random random = new Random();
		
		@Override
		public Boolean get() {
			return random.nextBoolean();
		}
	}
	
	/**
	 * Impose that the choose between the statements is random.
	 */
	public void setChooseRandomly() {
		this.condition = new RandomSupplier();
	}
	
	/**
	 * @return true if the choose between the statements is random.
	 */
	public boolean isChoosingRandomly() {
		return this.condition instanceof RandomSupplier;
	}
}
