package it.unica.co2.model;

import java.util.Arrays;
import java.util.Random;

public class Sum extends Process {

	private static final Random random = new Random();
	
	private final SumOperand[] operands;
	
	public Sum(Prefix... prefixes) {
		SumOperand[] operands = new SumOperand[prefixes.length];
		
		for (int i=0; i<prefixes.length; i++) {
			operands[i] = new SumOperand(prefixes[i]);
		}
		
		this.operands = operands;
	}
	
	public Sum(Prefix prefix, Process process) {
		operands = new SumOperand[]{new SumOperand(prefix,process)};
	}
	
	public Sum(SumOperand[] operands) {
		this.operands = operands;
	}

	@Override
	public void run() {
		
		if (operands.length==0) {
			System.out.println("empty sum");
			return;
		}
		
		if (operands.length==1) {
			this.execute(operands[0]);
			return;
		}
		
		System.out.println("sum of "+Arrays.asList(operands));
		
		int choice = random.nextInt(operands.length);
		SumOperand op = operands[choice];
		
		System.out.println("choosed operand: "+op);

		this.execute(op);
	}

	private void execute(SumOperand operand) {
		
		System.out.println("executing prefix: "+operand.getPrefix());
		try {
			operand.getPrefix().run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (operand.getProcess()!=null) {
			operand.getProcess().run();
		}
	}

	public SumOperand[] getOperands() {
		return operands;
	}
	
}
