package it.unica.co2.model.process;

import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.util.ObjectUtils;
import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;


public abstract class Participant extends Process {

	protected CO2ServerConnection connection;

	protected Participant(String username, String password) {
		super(username);
		
		try {
			logger.log("creating new connection: username=<"+username+"> password=<"+password+">");
			connection = new CO2ServerConnection(username, password);
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	abstract protected String getUsername();
	
	
	/*
	 * JPF-fields
	 */
	@SuppressWarnings("unused") private String serializedContract;
	@SuppressWarnings("unused") private String sessionName;
	
	protected Session2<TST> tell(Contract c) {
		try {
			serializedContract=ObjectUtils.serializeObjectToStringQuietly(c);
			
			TST tstA = new TST(c.toMiddleware());
			Private<TST> pvtA = tstA.toPrivate(connection);
			
			logger.log("telling contract <"+c+">");
			Public<TST> pbl = pvtA.tell();
			
			logger.log("waiting for session");
			Session<TST> session = pbl.waitForSession();
			
			Session2<TST> session2 = new Session2<TST>(connection, session.getContract());	//wrap to the session2
			logger.log("session fused");
			
			sessionName = session2.getSessionName();
			
			return session2;
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}
	
	
//	
//	
//	public void tau() {
//
//	}
//
//	public void doSend(Session2 session, String action) {
//		doSendPrefix(session, action).run();
//	}
//	
//	public void doSend(Session2 session, String action, String value) {
//		doSendPrefix(session, action, value).run();
//	}
//	
//	public void doSend(Session2 session, String action, Integer value) {
//		doSendPrefix(session, action, value).run();
//	}
//
//	public void doReceive(Session2 session, String... actions) {
//		doReceiveSum(session, actions).run();
//	}
//	
//	public void doReceive(Session2 session, Variable variable, String... actions) {
//		doReceiveSum(session, variable, actions).run();
//	}
	
//	public void sum(Prefix prefix, Process process) {
//		sumProcess(prefix, process).run();
//	}
//	
//	public void sum(Prefix... prefixes) {
//		sumProcess(prefixes).run();
//	}
//	
//	public void sum(SumOperand... ops) {
//		sumProcess(ops).run();
//	}
//	
//	public void sequence(Prefix prefix, Prefix... prefixes) {
//		sequenceProcess(prefix, prefixes).run();
//	}
//	
//	public void ifThenElse(Supplier<Boolean> condition, Process thenProcess, Process elseProcess) {
//		ifThenElseProcess(condition, thenProcess, elseProcess).run();
//	}
	
	
	/*
	 * 
	 * Prefix Factory
	 * 
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
//	public Prefix doSendPrefix(Session2 session, String action) {
//		return new DoSend(getUsername(),session, new Action(action, Sort.UNIT));
//	}
//	
//	public Prefix doSendPrefix(Session2 session, String action, String value) {
//		return new DoSend(getUsername(),session, new Action(action, Sort.STRING), value);
//	}
//	
//	public DoSend doSendPrefix(Session2 session, String action, Integer value) {
//		return new DoSend(getUsername(),session, new Action(action, Sort.INT), value.toString());
//	}
//	
//	public Sum doReceiveSum(Session2 session) {
//		return doReceiveSum(session, new Variable(Sort.UNIT));
//	}
//	
//	public Sum doReceiveSum(Session2 session, Variable variable) {
//		return new Sum(getUsername(), session, variable);
//	}
//	
//	public Sum doReceiveSum(Session2 session, String... actions) {
//		return doReceiveSum(session, new Variable(Sort.UNIT), actions);
//	}
//	
//	public Sum doReceiveSum(Session2 session, Variable variable, String... actions) {
//		Sum sum = new Sum(getUsername(), session, variable);
//		
//		for (String a : actions)
//			sum.add(new Action(a, variable.getSort()));
//		
//		return sum;
//	}
//	
//	public Sum doReceiveSum(Session2 session, Action... actions) {
//		return doReceiveSum(session, new Variable(Sort.UNIT), actions);
//	}
//	
//	public Sum doReceiveSum(Session2 session, Variable variable, Action... actions) {
//		Sum sum = new Sum(getUsername(), session, variable);
//		
//		for (Action a : actions)
//			sum.add(a);
//		
//		return sum;
//	}
//	
//	public Action action(String name) {
//		return action(name, Sort.UNIT);
//	}
//	
//	public Action action(String name, Sort sort) {
//		return new Action(name, sort);
//	}
	
	/*
	 * 
	 * Process Factory
	 * 
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
//	public Process tauProcess() {
//		return new Sum(getUsername(), tauPrefix());
//	}
//
//	public Process doSendProcess(Session2 session, String action) {
//		return new Sum(getUsername(), doSendPrefix(session, action));
//	}
//
//	public Process doSendProcess(Session2 session, String action, String value) {
//		return new Sum(getUsername(), doSendPrefix(session, action, value));
//	}
//	
//	public Process doSendProcess(Session2 session, String action, Integer value) {
//		return new Sum(getUsername(), doSendPrefix(session, action, value));
//	}
//	
//	public Process doReceiveProcess(Session2 session, String... actions) {
//		return new Sum(getUsername(), doReceivePrefix(session, actions));
//	}
//
//	public Process doReceiveProcess(Session2 session, Variable variable, String... actions) {
//		return new Sum(getUsername(), doReceivePrefix(session, variable, actions));
//	}
	
//	public Process sumProcess(Prefix... prefixes) {
//		return new Sum(getUsername(), prefixes);
//	}
//	
//	public Process sumProcess(SumOperand... ops) {
//		return new Sum(getUsername(), ops);
//	}
//	
//	public Process sumProcess(Prefix prefix, Process process) {
//		return sumProcess(new SumOperand(prefix, process));
//	}
//	
//	public SumOperand sumOperand(Prefix prefix) {
//		return new SumOperand(prefix);
//	}
//	
//	public SumOperand sumOperand(Prefix prefix, Process process) {
//		return new SumOperand(prefix, process);
//	}
	
	/*
	 * shortcut to create a chain of sum with single prefix
	 */
//	public Sum sequenceProcess(Prefix prefix, Prefix... prefixes) {
//		
//		if (prefixes.length==0) {
//			return new Sum(getUsername(), prefix);
//		}
//		else {
//			Process tmp = sequenceProcess(prefixes[0], Arrays.copyOfRange(prefixes, 1, prefixes.length));
//			return new Sum(getUsername(), prefix, tmp);
//		}
//	}

//	public IfThenElseProcess ifThenElseProcess(Supplier<Boolean> condition, Process thenProcess, Process elseProcess) {
//		return new IfThenElseProcess(getUsername(), thenProcess, elseProcess, condition);
//	}

	
}
