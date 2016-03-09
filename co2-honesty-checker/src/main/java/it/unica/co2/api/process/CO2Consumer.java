package it.unica.co2.api.process;

import java.util.function.Consumer;

import co2api.Message;

public abstract class CO2Consumer extends CO2Process implements SerializableConsumer<Message> {

	private static final long serialVersionUID = 1L;
	
	protected Message msg;
	
	@Override
	public void accept(Message msg) {
		this.msg = msg;
	}
	
	@Override
	abstract public void run();
	
	
	
	public static CO2Consumer doNothing() {
		return new CO2Consumer() {
			private static final long serialVersionUID = 1L;

			@Override
			public void run() {}
		};
	}
	
	public static CO2Consumer doThis(Consumer<Message> consumer) {
		return new CO2Consumer() {
			private static final long serialVersionUID = 1L;

			@Override
			public void run() {
				consumer.accept(msg);
			}
		};
	}
	
}
