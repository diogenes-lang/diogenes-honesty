package it.unica.co2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import co2api.ContractException;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.SessionI;
import co2api.TimeExpiredException;
import it.unica.co2.api.process.MultipleSessionReceiver;

@RunWith(PowerMockRunner.class)
public class MultipleSessionReceiverTest {

	// helper class that will be mocked
	public static class Consumers {
	
		public void consume(Message msg) {}
		public void consumeA(Message msg) {}
		public void consumeB(Message msg) {}
		public void consumeC(Message msg) {}
		public void exceptional(){}
	}
	
	@Mock private SessionI<?> x;
	@Mock private SessionI<?> y;
	@Mock private Consumers consumers;
	
	@Test
	public void simple() throws ContractException, ContractViolationException, TimeExpiredException {
		
		//prepare
		Message msg = new Message("a", "");
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consume, "a", "b", "c");
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
				
		
		//prepare
		msg = new Message("b", "");
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
				
		
		//prepare
		msg = new Message("c", "");
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
	}
	
	@Test
	public void redefineConsumer() throws ContractException, ContractViolationException, TimeExpiredException {
		
		//prepare
		Message msg = new Message("b", "");
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consumeA, "a");
		mReceiver.add(x, consumers::consumeA, "b");
		mReceiver.add(x, consumers::consumeB, "b");
		mReceiver.add(x, consumers::consumeC, "c");
		mReceiver.waitForReceive();
		
		//verify
		Mockito.verify(consumers, Mockito.times(0)).consumeA(msg);
		Mockito.verify(consumers, Mockito.times(1)).consumeB(msg);
		Mockito.verify(consumers, Mockito.times(0)).consumeC(msg);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void exception() throws ContractException, ContractViolationException, TimeExpiredException {
		
		//prepare
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenThrow(TimeExpiredException.class);
		
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consumeA, "a");
		mReceiver.add(x, consumers::consumeB, "b");
		mReceiver.add(x, consumers::consumeC, "c");
		try {
			mReceiver.waitForReceive(1000);
		}
		catch(TimeExpiredException e) {
			consumers.exceptional();
		}
		
		//verify
		Mockito.verify(consumers, Mockito.times(1)).exceptional();
		Mockito.verifyNoMoreInteractions(consumers);		// consumers must not be invoked
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void multiSession() throws ContractException, ContractViolationException, TimeExpiredException {
		
		//prepare
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenThrow(TimeExpiredException.class);
		Message msgY = new Message("c", "");
		Mockito.when(y.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msgY);
		
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consumeA, "a", "b");
		mReceiver.add(y, consumers::consumeC, "c", "d");
		mReceiver.waitForReceive();
		
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consumeC(msgY);
		Mockito.verifyNoMoreInteractions(consumers);		// consumers must not be invoked
	}

}
