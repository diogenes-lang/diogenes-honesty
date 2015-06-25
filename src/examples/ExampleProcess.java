


public class ExampleProcess {
	
	
	public static void main (String[] args) throws Exception {
		
		System.out.println("---------Example1---------");
		new Thread(new Example1()).start();
		
		System.out.println("---------Example2---------");
		new Thread(new Example2()).start();
	}
	
	
	
}
