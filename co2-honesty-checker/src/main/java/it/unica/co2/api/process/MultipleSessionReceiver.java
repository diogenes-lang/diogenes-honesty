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
	
	private Map<SessionI<? extends ContractModel>, Map<String, Integer>> sessionActionsMap = new HashMap<>();
	
	private List<Consumer<Message>> consumers = new ArrayList<>();
	
	@SuppressWarnings("unused")	private Consumer<Message>[] consumersArray;			// used by JPF
	@SuppressWarnings("unused")	private String serializedSessionActionsMap;			// used by JPF
	
	private static final int WAIT_RECEIVE_TIMEOUT = 1000;
	
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, String... actionNames) {
		return add(session, (msg)->{}, actionNames);
	}
	
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, final Consumer<Message> consumer, String... actionNames) {

		logger.debug("adding pair <{}, {}>", session, actionNames);
		
		int nextConsumerIndex = consumers.size();
		consumers.add(consumer);
		
		// create a map associating the given consumer with each action
		Map<String, Integer> newEntry = new HashMap<>();
		
		for (String action : actionNames) {
			newEntry.put(action, nextConsumerIndex);
			logger.debug("actions: <{}, {}>", action, nextConsumerIndex);
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
			throw new IllegalStateException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void waitForReceive(int timeout) throws TimeExpiredException {
		
		try {
			serializedSessionActionsMap = ObjectUtils.serializeObjectToString(sessionActionsMap);
			consumersArray = consumers.toArray(new Consumer[]{});
		}
		catch (IOException e2) {
			throw new RuntimeException(e2);
		}
		
		_waitForReceive(timeout);
	}
	
	private void _waitForReceive(int timeout) throws TimeExpiredException {
		
		
		long endtime = System.currentTimeMillis()+timeout;
	
		while(true) {
			
			for (Entry<SessionI<? extends ContractModel>, Map<String, Integer>> e : sessionActionsMap.entrySet()) {
				
				// check if the timeout is expired
				if (timeout!=-1 && System.currentTimeMillis() > endtime) {
					logger.info("timeout expired: no action received from any session");
					throw new TimeExpiredException("no action received from any session");
				}
				
				SessionI<? extends ContractModel> session = e.getKey();
				Map<String, Integer> actionConsumersMap = e.getValue();
				Collection<String> actions = actionConsumersMap.keySet();
				
				// wait for receive a message
				try {
					Message msg = session.waitForReceive(WAIT_RECEIVE_TIMEOUT, actions.toArray(new String[]{}));
					
					String action = msg.getLabel();
					assert actions.contains(action);
					assert actionConsumersMap.containsKey(action);
					
					int consumerIndex = actionConsumersMap.get(action);
					consumers.get(consumerIndex).accept(msg);
					return;
				}
				catch (TimeExpiredException e1) {
					logger.debug("session {} does not receive any message within the given delay ({} msec)", session, WAIT_RECEIVE_TIMEOUT);
				}
			}
		}
		
	}
	
}
