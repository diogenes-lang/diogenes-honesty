package it.unica.co2.model;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.model.contract.RecursionReference;
import it.unica.co2.model.contract.Action.Sort;

import java.util.Arrays;

public class Factory {
	
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
		doReceiveProcess(session, action).run();
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
	
	
	public static SumOperand sumOperand(Prefix prefix) {
		return new SumOperand(prefix);
	}
	
	public static SumOperand sumOperand(Prefix prefix, Process process) {
		return new SumOperand(prefix, process);
	}
	
	public static void sequence(Prefix prefix, Prefix... prefixes) {
		sequenceProcess(prefix, prefixes).run();
	}

	
	
	
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
	
	public static Process sumProcess(Prefix... prefixes) {
		return new Sum(prefixes);
	}
	
	public static Process sumProcess(SumOperand... ops) {
		return new Sum(ops);
	}
	
	public static Process sumProcess(Prefix prefix, Process process) {
		return sumProcess(new SumOperand(prefix, process));
	}
	
	/*
	 * shortcut to create a chain of sum with single prefix
	 */
	public static Process sequenceProcess(Prefix prefix, Prefix... prefixes) {
		
		if (prefixes.length==0) {
			return new Sum(prefix);
		}
		else {
			Process tmp = sequenceProcess(prefixes[0], Arrays.copyOfRange(prefixes, 1, prefixes.length));
			return new Sum(prefix, tmp);
		}
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
		InternalAction[] actions = 
				Arrays.stream(actionNames)
				.map( p -> new InternalAction(p, sort) )
				.toArray(InternalAction[]::new);
		
		return new InternalSum(actions);
	}
	
	
	
	
	public static ExternalSum externalSum(String... actionNames) {
		return externalSum(Sort.UNIT, actionNames);
	}
	
	public static ExternalSum externalSum(Sort sort, String... actionNames) {
		ExternalAction[] actions = 
				Arrays.stream(actionNames)
				.map( p -> new ExternalAction(p, sort) )
				.toArray(ExternalAction[]::new);
		
		return new ExternalSum(actions);
	}

	
	
	
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
	
	public static Recursion recursion(String recursionName, Contract contract) {
		return new Recursion(recursionName, contract);
	}
	
	public static RecursionReference recursionReference(String referredRecursionName) {
		return new RecursionReference(referredRecursionName);
	}
}
