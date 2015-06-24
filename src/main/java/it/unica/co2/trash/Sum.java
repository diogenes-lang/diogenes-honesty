package it.unica.co2.trash;

import it.unica.co2.model.prefix.Prefix;
import it.unica.co2.model.process.Process;

import java.util.Arrays;
import java.util.Random;

public class Sum extends Process {

	private static final Random random = new Random();
	
	private final SumOperand[] operands;
	
	public Sum(String username, Prefix... prefixes) {
		super(username);
		
		SumOperand[] operands = new SumOperand[prefixes.length];
		
		for (int i=0; i<prefixes.length; i++) {
			operands[i] = new SumOperand(prefixes[i]);
		}
		
		this.operands = operands;
	}
	
	public Sum(String username, Prefix prefix, Process process) {
		super(username);
		operands = new SumOperand[]{new SumOperand(prefix,process)};
	}
	
	public Sum(String username, SumOperand[] operands) {
		super(username);
		this.operands = operands;
	}

	@Override
	public void run() {
		
		if (operands.length==0) {
			logger.log("empty sum");
			return;
		}
		
		if (operands.length==1) {
			this.execute(operands[0]);
			return;
		}
		
		logger.log("sum of "+Arrays.asList(operands));
		
		int choice = random.nextInt(operands.length);
		SumOperand op = operands[choice];
		
		logger.log("choosed operand: "+op);

		this.execute(op);
	}

	private void execute(SumOperand operand) {
		
		logger.log("executing prefix: "+operand.getPrefix());
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
