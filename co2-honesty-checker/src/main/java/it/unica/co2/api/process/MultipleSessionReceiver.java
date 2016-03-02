package it.unica.co2.api.process;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co2api.ContractModel;
import co2api.Message;
import co2api.Session;
import co2api.TimeExpiredException;

public class MultipleSessionReceiver {

	private static final Logger logger = LoggerFactory.getLogger(MultipleSessionReceiver.class);
	
	private Map<Session<? extends ContractModel>, List<String>> sessionActionsMap = new HashMap<>();
	
	private static final int WAIT_RECEIVE_TIMEOUT = 1000;
	
	public MultipleSessionReceiver add(Session<? extends ContractModel> session, String... actionNames) {

		logger.debug("adding pair {}, {}", session, actionNames);
		
		sessionActionsMap.merge(
				session, 
				Arrays.asList(actionNames),
				(x,y)-> {
					x.addAll(y);
					return x;
				});
		
		return this;
	}
	
	
	public MessageWrapper waitForReceive() throws TimeExpiredException {
		return waitForReceive(-1);
	}
	
	public MessageWrapper waitForReceive(int timeout) throws TimeExpiredException {
		
		long endtime = System.currentTimeMillis()+timeout;
	
		while(true) {
			
			for (Entry<Session<? extends ContractModel>, List<String>> e : sessionActionsMap.entrySet()) {
				
				// check if the timeout is expired
				if (timeout!=-1 && System.currentTimeMillis() > endtime) {
					logger.info("timeout expired");
					throw new TimeExpiredException("no action received from any session");
				}
				
				Session<? extends ContractModel> session = e.getKey();
				List<String> actions = e.getValue();
				
				// wait for receive a message
				try {
					Message msg = session.waitForReceive(WAIT_RECEIVE_TIMEOUT, actions.toArray(new String[]{}));
					return new MessageWrapper(session, msg);
				}
				catch (TimeExpiredException e1) {
					logger.debug("session {} does not receive any message within the given delay ({} msec)", session, WAIT_RECEIVE_TIMEOUT);
				}
			}
		}
		
	}
	
	public static class MessageWrapper {
		private final Session<? extends ContractModel> session;
		private final Message message;
		
		public MessageWrapper(Session<? extends ContractModel> session, Message message) {
			this.session = session;
			this.message = message;
		}

		public Session<? extends ContractModel> getSession() {
			return session;
		}
		
		public Message getMessage() {
			return message;
		}
	}
}
