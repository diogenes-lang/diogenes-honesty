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
	
	private static long honestyStartTime;
	private static long honestyEndTime;
	private static long jpfStartTime;
	private static long jpfEndTime;
	private static long maudeStartTime;
	private static long maudeEndTime;
	
	public static void update(Event event) {
		trace(event);
	}
	
	
	private static void trace(Event event) {
		
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

	public static long getTotalTime() {
		return honestyEndTime-honestyStartTime;
	}
	
	public static long getJPFTime() {
		return jpfEndTime-jpfStartTime;
	}
	
	public static long getMaudeTime() {
		return maudeEndTime-maudeStartTime;
	}
}
