package it.unica.co2.examples;

import java.util.Arrays;

public class JPFLambda {
	
	public static void main (String[] args) throws Exception {
		
		
		Arrays.stream(new Integer[]{1,2,3,4,5,6,7,8,9,10})
			.map( p -> p+1 )
			.forEach((x)->{
				System.out.println(x);
			});
		
	}
	
}
