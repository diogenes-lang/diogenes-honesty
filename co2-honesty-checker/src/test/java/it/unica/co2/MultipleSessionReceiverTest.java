package it.unica.co2;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import co2api.ContractException;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;
import it.unica.co2.api.process.MultipleSessionReceiver;

@RunWith(MockitoJUnitRunner.class)
public class MultipleSessionReceiverTest {

	// helper class that will be mocked
	public static class Consumers {
		public void consume(Message msg) {}
		public void consumeA(Message msg) {}
		public void consumeB(Message msg) {}
		public void consumeC(Message msg) {}
		public void exceptional(){}
	}
	
	@Mock private Public<?> pblx;
	@Mock private Session<?> x;
	@Mock private Session<?> y;
	@Mock private Consumers consumers;
	
	@Before
	public void before() {
		Mockito.when(x.getContractID()).thenReturn("x");
		Mockito.when(y.getContractID()).thenReturn("y");
		Mockito.when(pblx.getContractID()).thenReturn("x_pbl");
	}
	
	@Test
	public void simple() throws ContractException, ContractViolationException, TimeExpiredException {
		
		System.out.println("simple()");
		
		//prepare
		Message msg = new Message("a", "", x);
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consume, "a", "b", "c");
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
				
		
		//prepare
		msg = new Message("b", "", x);
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
				
		
		//prepare
		msg = new Message("c", "", x);
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msg);
		//test
		mReceiver.waitForReceive();
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consume(msg);
	}
	
	@Test
	public void redefineConsumer() throws ContractException, ContractViolationException, TimeExpiredException {
		
		System.out.println("redefineConsumer()");
		
		//prepare
		Message msg = new Message("b", "", x);
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
		
		System.out.println("exception()");
		
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
		
		System.out.println("multiSession()");
		
		//prepare
		Mockito.when(y.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenThrow(TimeExpiredException.class);
		
		Message msgX = new Message("a", "", x);
		Mockito.when(x.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msgX);
		
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(x, consumers::consumeA, "a", "b");
		mReceiver.add(y, consumers::consumeC, "c", "d");
		mReceiver.waitForReceive();
		
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consumeA(msgX);
		Mockito.verifyNoMoreInteractions(consumers);		// consumers must not be invoked
	}
	
	@Test
	public void multiSession2() throws ContractException, ContractViolationException, TimeExpiredException {
		
		System.out.println("multiSession2()");
		
		//prepare
		Mockito.when(pblx.isFused()).thenReturn(false);
		
		Message msgY = new Message("c", "", y);
		Mockito.when(y.waitForReceive(Mockito.anyInt(), Mockito.anyVararg())).thenReturn(msgY);
		
		//test
		MultipleSessionReceiver mReceiver = new MultipleSessionReceiver();
		mReceiver.add(pblx, consumers::consumeA, "a", "b");
		mReceiver.add(y, consumers::consumeC, "c", "d");
		mReceiver.waitForReceive();
		
		//verify
		Mockito.verify(consumers, Mockito.times(1)).consumeC(msgY);
		Mockito.verifyNoMoreInteractions(consumers);		// consumers must not be invoked
	}

}
