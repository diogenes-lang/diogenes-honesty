package it.unica.co2.api.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co2api.ContractModel;
import co2api.Message;
import co2api.SessionI;
import co2api.TimeExpiredException;
import it.unica.co2.util.ObjectUtils;

public class MultipleSessionReceiver {

	private static final Logger logger = LoggerFactory.getLogger(MultipleSessionReceiver.class);
	
	private Map<SessionI<? extends ContractModel>, List<String>> sessionActionsMap = new HashMap<>();
	private Map<String, Consumer<Message>> consumersMap = new HashMap<>();			// the key is <sessionID>$<action>
	
	@SuppressWarnings("unused")	private String serializedSessionActionsMap;			// used by JPF
	
	
	private static final int WAIT_RECEIVE_TIMEOUT = 1000;
	
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, String... actionNames) {
		return add(session, new Consumer<Message>() {

			@Override
			public void accept(Message t) {}
			
		}, actionNames);
	}
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, final Consumer<Message> consumer, String... actionNames) {

		assert session.getSessionID()!=null;

		logger.debug("adding pair <{}, {}>", session, actionNames);
		
		// create a map associating the given consumer with each action
		
		for (String action : actionNames) {
			consumersMap.put(session.getSessionID()+"$"+action, consumer);

			sessionActionsMap.putIfAbsent(session, new ArrayList<>());
			sessionActionsMap.get(session).add(action);
		}
		
		return this;
	}
	
	
	public void waitForReceive() {
		try {
			waitForReceive(-1);
		}
		catch (TimeExpiredException e) {
			// unreachable
			throw new IllegalStateException(e);
		}
	}
	
	public void waitForReceive(int timeout) throws TimeExpiredException {
		
		try {
			serializedSessionActionsMap = ObjectUtils.serializeObjectToString(sessionActionsMap);
		}
		catch (IOException e2) {
			throw new RuntimeException(e2);
		}
		
		Message msg = _waitForReceive(timeout);
		
		// dispatch to consumer
		String action = msg.getLabel();
		String sessionID = msg.getSessionID();
		
		Consumer<Message> consumer = consumersMap.get(sessionID+"$"+action);
		assert consumer != null;
		
		consumer.accept(msg);
	}
	
	public Message _waitForReceive(int timeout) throws TimeExpiredException {
		
		
		long endtime = System.currentTimeMillis()+timeout;
	
		while(true) {
			
			for (Entry<SessionI<? extends ContractModel>, List<String>> e : sessionActionsMap.entrySet()) {
				
				// check if the timeout is expired
				if (timeout!=-1 && System.currentTimeMillis() > endtime) {
					logger.info("timeout expired: no action received from any session");
					throw new TimeExpiredException("no action received from any session");
				}
				
				SessionI<? extends ContractModel> session = e.getKey();
				Collection<String> actions = e.getValue();
				
				// wait for receive a message
				try {
					Message msg = session.waitForReceive(WAIT_RECEIVE_TIMEOUT, actions.toArray(new String[]{}));
					return msg;
				}
				catch (TimeExpiredException e1) {
					logger.debug("session {} does not receive any message within the given delay ({} msec)", session, WAIT_RECEIVE_TIMEOUT);
				}
			}
		}
		
	}
	
}
