package it.unica.co2;

import java.util.Arrays;



public class Example {

	
	public static void main (String[] args) throws Exception {
		
//		new ExampleProcess().run();
		System.out.println(
				Arrays.toString(
					Arrays.stream(new Integer[]{1,2,3,4,5,6,7,8,9,10})
						.map( p -> p+1 )
						.toArray(Integer[]::new)
				)
		);
	}
	
}
