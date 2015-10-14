package it.unica.co2.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TEST {
	
	public static void main (String[] args) {
		
//		ContractDefinition C = def("C");
//		ContractDefinition Cread = def("Cread");
//		ContractDefinition Cwrite = def("Cwrite");
//		
//		C.setContract(externalSum().add("req", Sort.STRING, internalSum().add("ackR", Sort.UNIT, ref(Cread)).add("ackW", Sort.UNIT, ref(Cwrite)).add("error", Sort.UNIT)));
//		Cread.setContract(internalSum().add("data", Sort.INT, externalSum().add("ack", Sort.INT, ref(Cread))).add("error", Sort.UNIT));
//		Cwrite.setContract(empty());
//		
//		System.out.println(C.getContract());
//		System.out.println(C.getContract().toMaude());
//		System.out.println(C.getContract().toTST());
		
		
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
        
        TEST.fooMethod(
                foo1,
                (x)-> {
                	List<Integer> lst = new ArrayList<>();
                	System.out.println("[lambda] lst.hashCode(): "+lst.hashCode());

                	System.out.println("[lambda] this.hashCode(): "+this.hashCode());
                    System.out.println("[lambda] x.hashCode(): "+x.hashCode());
                    
                    TEST.fooMethod(
                    		x,
                    		(y)-> {
                    			System.out.println("[lambda2] lst.hashCode(): "+lst.hashCode());
                    			System.out.println("[lambda2] this.hashCode(): "+this.hashCode());
                    			System.out.println("[lambda2] x.hashCode(): "+x.hashCode());
                    			System.out.println("[lambda2] y.hashCode(): "+y.hashCode());
                    		});
                    
                });
       
        return this;
    }
   
}
