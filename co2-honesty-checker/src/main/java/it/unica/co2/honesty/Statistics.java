package it.unica.co2.honesty;

public class Statistics {

	public enum Event {
		HONESTY_START,
		HONESTY_END,
		JPF_START,
		JPF_END,
		MAUDE_START,
		MAUDE_END
	}
	
	private long honestyStartTime;
	private long honestyEndTime;
	private long jpfStartTime;
	private long jpfEndTime;
	private long maudeStartTime;
	private long maudeEndTime;
	
	public void update(Event event) {
		trace(event);
	}
	
	
	private void trace(Event event) {
		
		switch (event) {
		
		case HONESTY_START:
			honestyStartTime = System.currentTimeMillis();
			break;
			
		case HONESTY_END:
			honestyEndTime = System.currentTimeMillis();
			break;
		
		case JPF_START:
			jpfStartTime = System.currentTimeMillis();
			break;
			
		case JPF_END:
			jpfEndTime = System.currentTimeMillis();
			break;
		
		case MAUDE_START:
			maudeStartTime = System.currentTimeMillis();
			break;
			
		case MAUDE_END:
			maudeEndTime = System.currentTimeMillis();
			break;
		}
	}

	public long getTotalTime() {
		return honestyEndTime-honestyStartTime;
	}
	
	public long getJPFTime() {
		return jpfEndTime-jpfStartTime;
	}
	
	public long getMaudeTime() {
		return maudeEndTime-maudeStartTime;
	}
}
