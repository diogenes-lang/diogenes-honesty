# Installation notes #
## Eclipse projects ##
* `co2-fake-api`: fake implementation of co2 low-level api (`CO2ServerConnection.java`)
* `co2-honesty-checker`: core project that test the honesty of java processes

Import as "Existing projects into Workspace". There isn't any build management at the moment.

## Configuration ##
You must set some properties into [jpf.properties](/co2-honesty-checker/src/main/resources/jpf.properties):

* `honesty.maude.co2-maude = <absolute path of the co2-maude folder>`
* `honesty.maude.exec = <absolute path of the executable maude file>`

# Examples #
All examples are under [src/examples](co2-honesty-checker/src/examples/).

## SimpleBuyer process ##
```
#!java
package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;


public class SimpleBuyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "alice@test.com";
	private static final String password = "alice";
	
	public SimpleBuyer() {
		super(username, password);
	}

	
	private Contract contract = 
			internalSum().add(
					"item",
					externalSum().add("amount",
							internalSum()
							.add("pay", externalSum().add("item"))
							.add("abort")
							)
			);
	
	@Override
	public void run() {
		
		try {
			Session2<TST> session = tellAndWait(contract);
			
			session.send("item", "01234");
			
			Message msg = session.waitForReceive("amount");
			
			Integer n = Integer.valueOf(msg.getStringValue());
			
			if (n<10) {
				session.send("pay", n);
				msg = session.waitForReceive("item");
			}
			else {
				session.send("abort");
			}
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		new SimpleBuyer().run();
	}

}
```
We check the honesty with
```
#!java
@Test
public void simpleBuyer() {
	boolean honesty = HonestyChecker.isHonest(SimpleBuyer.class);
	assertTrue(honesty);
}
```

