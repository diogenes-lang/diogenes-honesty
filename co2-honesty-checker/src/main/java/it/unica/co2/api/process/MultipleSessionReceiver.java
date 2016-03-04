package it.unica.co2.api.process;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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
	
	private Map<SessionI<? extends ContractModel>, Map<String, Consumer<Message>>> sessionActionsMap = new HashMap<>();
	
	@SuppressWarnings("unused")
	private String serializedSessionActionsMap;
	
	private static final int WAIT_RECEIVE_TIMEOUT = 1000;
	
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, String... actionNames) {
		return add(session, (x)->{}, actionNames);
	}
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, final SerializableConsumer<Message> consumer, String... actionNames) {

		logger.debug("adding pair <{}, {}>", session, actionNames);
		
		// create a map associating the given consumer with each action
		Map<String, Consumer<Message>> newEntry = new HashMap<>();
		
		for (String action : actionNames) {
			newEntry.put(action, consumer);
		}

		sessionActionsMap.merge(
				session, 
				newEntry,
				(x, y)-> {				// put all the new entries into the old map
					x.putAll(y);
					return x;
				});
		
		return this;
	}
	
	
	public void waitForReceive() {
		try {
			waitForReceive(-1);
		}
		catch (TimeExpiredException e) {
			// unreachable
			throw new RuntimeException(e);
		}
	}
	
	public void waitForReceive(int timeout) throws TimeExpiredException {
		
		try {
			serializedSessionActionsMap = ObjectUtils.serializeObjectToString(sessionActionsMap);
		}
		catch (IOException e2) {
			throw new RuntimeException(e2);
		}
		_waitForReceive(timeout);
	}
	
	private void _waitForReceive(int timeout) throws TimeExpiredException {
		
		
		long endtime = System.currentTimeMillis()+timeout;
	
		while(true) {
			
			for (Entry<SessionI<? extends ContractModel>, Map<String, Consumer<Message>>> e : sessionActionsMap.entrySet()) {
				
				// check if the timeout is expired
				if (timeout!=-1 && System.currentTimeMillis() > endtime) {
					logger.info("timeout expired: no action received from any session");
					throw new TimeExpiredException("no action received from any session");
				}
				
				SessionI<? extends ContractModel> session = e.getKey();
				Map<String, Consumer<Message>> consumers = e.getValue();
				Collection<String> actions = consumers.keySet();
				
				// wait for receive a message
				try {
					Message msg = session.waitForReceive(WAIT_RECEIVE_TIMEOUT, actions.toArray(new String[]{}));
					
					String action = msg.getLabel();
					assert actions.contains(action);
					assert consumers.containsKey(action);
					
					consumers.get(action).accept(msg);
					return;
				}
				catch (TimeExpiredException e1) {
					logger.debug("session {} does not receive any message within the given delay ({} msec)", session, WAIT_RECEIVE_TIMEOUT);
				}
			}
		}
		
	}
	
}
