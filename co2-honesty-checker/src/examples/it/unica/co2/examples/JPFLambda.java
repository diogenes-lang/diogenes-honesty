package it.unica.co2.examples;

import java.util.function.Consumer;

public class JPFLambda {
	
	public static void main (String[] args) {
		
		Foo foo = new Foo();
		
		new Foo().test(foo);
		System.out.println("---------------------------------");
		new Foo().test(foo);
	}
	
	public static void fooMethod(Foo foo, Consumer<Foo> consumer) {
		consumer.accept(foo);
	}
}

class Foo {
	
	public Foo test(Foo foo1) {

		System.out.println("this.hashCode(): "+this.hashCode());
		System.out.println("foo1.hashCode(): "+foo1.hashCode());
		
		JPFLambda.fooMethod(
				foo1, 
				(x)-> {
					System.out.println("[lambda] this.hashCode(): "+this.hashCode());
					System.out.println("[lambda] x.hashCode(): "+x.hashCode());
				});
		
		return this;
	}
	
}