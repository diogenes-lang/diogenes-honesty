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
import co2api.Public;
import co2api.SessionI;
import co2api.TimeExpiredException;
import it.unica.co2.util.ObjectUtils;

/**
 * This class allows to receive a {@code Message} from multiple {@code SessionI}.
 * The algorithm is implemented as polling. 
 * 
 * @author Nicola Atzei
 *
 */
public class MultipleSessionReceiver {

	private static final Logger logger = LoggerFactory.getLogger(MultipleSessionReceiver.class);
	
	private Map<SessionI<? extends ContractModel>, List<String>> sessionActionsMap = new HashMap<>();
	private Map<String, Map<String, Consumer<Message>>> sessionConsumersMap = new HashMap<>();
	
	@SuppressWarnings("unused")	private String serializedSessionActionsMap;			// used by JPF
	
	/*
	 * timeout polling
	 */
	private static final int POLLING_TIMEOUT = 1000;
	
	/**
	 * Add the given {@code session}, from which the given {@code actionNames} are expected to come.
	 * <p>This method is an <em>initialization</em> method.
	 * 
	 * @param session The session, already fused or not.
	 * @param actionNames The action names expected to come from the session
	 * @return this object
	 * 
	 * @see MultipleSessionReceiver#waitForReceive()
	 * @see MultipleSessionReceiver#waitForReceive(int)
	 */
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, String... actionNames) {
		return add(session, (x)->{}, actionNames);
	}
	
	/**
	 * Add the given {@code session}, from which the given {@code action} is expected to come.
	 * The given {@code consumer} is executed if the given action is received from the session.
	 * <p>This method is an <em>initialization</em> method.
	 * 
	 * @param session The session, already fused or not.
	 * @param action The action name expected to come from the session
	 * @param consumer The consumer to execute if the action is received
	 * @return this object
	 * 
	 * @see MultipleSessionReceiver#waitForReceive()
	 * @see MultipleSessionReceiver#waitForReceive(int)
	 */
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, String action, final Consumer<Message> consumer) {
		return add(session, consumer, new String[]{action});
	}
	
	/**
	 * Add the given {@code session}, from which the given {@code actionNames} are expected to come.
	 * The given {@code consumer} is executed if any of the given action is received from the session.
	 * <p>This method is an <em>initialization</em> method.
	 * 
	 * @param session The session, already fused or not.
	 * @param actionNames The action names expected to come from the session
	 * @param consumer The consumer to execute if an action is received
	 * @return this object
	 * 
	 * @see MultipleSessionReceiver#waitForReceive()
	 * @see MultipleSessionReceiver#waitForReceive(int)
	 */
	public MultipleSessionReceiver add(SessionI<? extends ContractModel> session, final Consumer<Message> consumer, String... actionNames) {

		logger.debug("adding pair <{}, {}>", session, actionNames);
		
		// create a map associating the given consumer with each action
		
		HashMap<String, Consumer<Message>> newEntries = new HashMap<>();
		
		for (String action : actionNames) {
			newEntries.put(action, consumer);

			sessionActionsMap.putIfAbsent(session, new ArrayList<>());
			sessionActionsMap.get(session).add(action);
		}
		
		sessionConsumersMap.merge(
				session.getContractID(),
				newEntries, 
				(x,y)-> {
					x.putAll(newEntries);
					return x;
				});
		
		return this;
	}
	
	/**
	 * Start polling between sessions and wait until a specified action come. 
	 */
	public void waitForReceive() {
		try {
			waitForReceive(-1);
		}
		catch (TimeExpiredException e) {
			// unreachable
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Start polling between sessions and wait until a specified action come or the timeout expires.
	 *
	 * @param timeout msec
	 * @throws TimeExpiredException if the timeout expires
	 */
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
		String contractID = msg.getSession().getContractID();
		
		logger.debug("received message on session/contract {}, action {}", contractID, action);
		System.out.println("received message on session/contract "+contractID+", action "+action);
		System.out.println("sessionConsumersMap: "+sessionConsumersMap);
		
		// get the consumer
		Consumer<Message> consumer = sessionConsumersMap.get(contractID).get(action);
		assert consumer != null;
		
		consumer.accept(msg);
	}
	
	/*
	 * internal implementation of the receive
	 */
	private Message _waitForReceive(int timeout) throws TimeExpiredException {
		
		
		long endtime = System.currentTimeMillis()+timeout;
	
		logger.debug("sessions: {}", sessionActionsMap);
		logger.debug("consumers: {}", sessionConsumersMap);
		
		while(true) {
			
			for (Entry<SessionI<? extends ContractModel>, List<String>> e : sessionActionsMap.entrySet()) {
				
				// check if the timeout is expired
				if (timeout!=-1 && System.currentTimeMillis() > endtime) {
					logger.info("timeout expired: no action received from any session");
					throw new TimeExpiredException("no action received from any session");
				}
				
				SessionI<? extends ContractModel> session = e.getKey();
				Collection<String> actions = e.getValue();
				
				
				// check if the session is established
				if (
						(session instanceof Public)
						&& !(((Public<? extends ContractModel>) session).isFused())) {
					logger.debug("session {} not fused yet", session);
				}
				else {
					// if the class is Session, then the contract was already fused
					
					logger.debug("session {} is fused", session);
					
					try {
						// wait for receive a message
						Message msg = session.waitForReceive(POLLING_TIMEOUT, actions.toArray(new String[]{}));
						return msg;
					}
					catch (TimeExpiredException e1) {
						logger.debug("session {} does not receive any message within the given delay ({} msec)", session, POLLING_TIMEOUT);
					}
				}
			}
		}
	}
	
}
