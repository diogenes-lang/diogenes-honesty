package it.unica.co2.model;

import it.unica.co2.model.contract.Action.Sort;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.model.prefix.DoReceive;
import it.unica.co2.model.prefix.DoSend;
import it.unica.co2.model.prefix.Prefix;
import it.unica.co2.model.prefix.Tau;
import it.unica.co2.model.prefix.Tell;
import it.unica.co2.model.prefix.Variable;
import it.unica.co2.model.process.IfThenElseProcess;
import it.unica.co2.model.process.Process;
import it.unica.co2.model.process.Sum;
import it.unica.co2.model.process.SumOperand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CO2Factory {
	
	public static void tau() {
		tauProcess().run();
	}

	public static void tell(String session, Contract contract) {
		tellProcess(session, contract).run();
	}

	public static void doSend(String session, String action, String value) {
		doSendProcess(session, action, value).run();
	}

	public static void doReceive(String session, String action) {
		doReceive(session, action, null);
	}
	
	public static void doReceive(String session, String action, Variable receivedValue) {
		doReceiveProcess(session, action, receivedValue).run();
	}
	
	public static void sum(Prefix prefix, Process process) {
		sumProcess(prefix, process).run();
	}
	
	public static void sum(Prefix... prefixes) {
		sumProcess(prefixes).run();
	}
	
	public static void sum(SumOperand... ops) {
		sumProcess(ops).run();
	}
	
	public static void sequence(Prefix prefix, Prefix... prefixes) {
		sequenceProcess(prefix, prefixes).run();
	}
	
	public static void ifThenElse(Process thenProcess, Process elseProcess) {
		ifThenElseProcess(thenProcess, elseProcess).run();
	}
	
	public static void ifThenElse(Supplier<Boolean> condition, Process thenProcess, Process elseProcess) {
		ifThenElseProcess(condition, thenProcess, elseProcess).run();
	}
	
	
	/*
	 * 
	 * Prefix Factory
	 * 
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	public static Prefix tauPrefix() {
		return new Tau();
	}

	public static Prefix tellPrefix(String session, Contract contract) {
		return new Tell(session, contract);
	}
	
	public static Prefix doSendPrefix(String session, String action) {
		return new DoSend(session, action);
	}
	
	public static Prefix doSendPrefix(String session, String action, String value) {
		return new DoSend(session, action, value);
	}

	public static Prefix doReceivePrefix(String session, String action) {
		return new DoReceive(session, action);
	}
	
	
	
	/*
	 * 
	 * Process Factory
	 * 
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	public static Process tauProcess() {
		return new Sum(new Tau());
	}

	public static Process tellProcess(String session, Contract contract) {
		return new Sum(new Tell(session, contract));
	}
	
	public static Process doSendProcess(String session, String action) {
		return doSendProcess(session, action, null);
	}

	public static Process doSendProcess(String session, String action, String value) {
		return new Sum(new DoSend(session, action, value));
	}

	public static Process doReceiveProcess(String session, String action) {
		return new Sum(new DoReceive(session, action));
	}
	
	public static Process doReceiveProcess(String session, String action, Variable variable) {
		return new Sum(new DoReceive(session, action, variable));
	}
	
	public static Process sumProcess(Prefix... prefixes) {
		return new Sum(prefixes);
	}
	
	public static Process sumProcess(SumOperand... ops) {
		return new Sum(ops);
	}
	
	public static Process sumProcess(Prefix prefix, Process process) {
		return sumProcess(new SumOperand(prefix, process));
	}
	
	public static SumOperand sumOperand(Prefix prefix) {
		return new SumOperand(prefix);
	}
	
	public static SumOperand sumOperand(Prefix prefix, Process process) {
		return new SumOperand(prefix, process);
	}
	
	/*
	 * shortcut to create a chain of sum with single prefix
	 */
	public static Sum sequenceProcess(Prefix prefix, Prefix... prefixes) {
		
		if (prefixes.length==0) {
			return new Sum(prefix);
		}
		else {
			Process tmp = sequenceProcess(prefixes[0], Arrays.copyOfRange(prefixes, 1, prefixes.length));
			return new Sum(prefix, tmp);
		}
	}

	public static IfThenElseProcess ifThenElseProcess(Process thenProcess, Process elseProcess) {
		return new IfThenElseProcess(thenProcess, elseProcess);
	}
	
	public static IfThenElseProcess ifThenElseProcess(Supplier<Boolean> condition, Process thenProcess, Process elseProcess) {
		return new IfThenElseProcess(thenProcess, elseProcess, condition);
	}

	
	
	
	
	/*
	 * 
	 * Contract Factory 
	 * 
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	public static InternalAction internalAction(String actionName) {
		return internalAction(actionName, null);
	}
	
	public static InternalAction internalAction(String actionName, Contract next) {
		return internalAction(Sort.UNIT, actionName, next);
	}
	
	public static InternalAction internalAction(Sort sort, String actionName, Contract next) {
		return new InternalAction(actionName, sort, next);
	}
	
	public static ExternalAction externalAction(String actionName) {
		return externalAction(actionName, null);
	}
	
	public static ExternalAction externalAction(String actionName, Contract next) {
		return externalAction(Sort.UNIT, actionName, next);
	}
	public static ExternalAction externalAction(Sort sort, String actionName, Contract next) {
		return new ExternalAction(actionName, sort, next);
	}
	
	public static Recursion recursion() {
		return new Recursion();
	}
	
	public static InternalSum internalSum(InternalAction... actions) {
		return new InternalSum(actions);
	}
	
	public static ExternalSum externalSum(ExternalAction... actions) {
		return new ExternalSum(actions);
	}
	
	
	
	public static InternalSum internalSum(String... actionNames) {
		return internalSum(Sort.UNIT, actionNames);
	}
	
	public static InternalSum internalSum(Sort sort, String... actionNames) {
//		InternalAction[] actions = 
//				Arrays.stream(actionNames)
//				.map( p -> new InternalAction(p, sort) )
//				.toArray(InternalAction[]::new);
		
		List<InternalAction> actions = new ArrayList<>();
		
		for (String action : actionNames) {
			actions.add( new InternalAction(action, sort) );
		}
		
		return new InternalSum(actions.toArray(new InternalAction[]{}));
	}
	
	
	
	
	public static ExternalSum externalSum(String... actionNames) {
		return externalSum(Sort.UNIT, actionNames);
	}
	
	public static ExternalSum externalSum(Sort sort, String... actionNames) {
//		ExternalAction[] actions = 
//				Arrays.stream(actionNames)
//				.map( p -> new ExternalAction(p, sort) )
//				.toArray(ExternalAction[]::new);
		
		List<ExternalAction> actions = new ArrayList<>();
		
		for (String action : actionNames) {
			actions.add( new ExternalAction(action, sort) );
		}
		
		return new ExternalSum(actions.toArray(new ExternalAction[]{}));
	}
}
