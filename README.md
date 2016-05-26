# Installation notes #
## Eclipse project ##
* `co2-honesty-checker`: core project that test the honesty of java processes

Import as "Existing projects into Workspace". There isn't any build management at the moment.

## Configuration ##
You must set two mandatory properties into [co2.properties](/co2-honesty-checker/src/main/resources/co2.properties):

* `honesty.maude.co2-maude` : the absolute path to the co2 maude files that check the honesty
* `honesty.maude.exec` : the absolute path to the maude executable

## SimpleBuyer process ##
```
#!java

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
			Session<TST> session = tellAndWait(contract);
			
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
HonestyChecker.isHonest(SimpleBuyer.class);
```

#Maven

Create JPF packages to use it within maven.
```
mvn install:install-file -Dfile=jpf.jar -DgroupId=jpf -DartifactId=jpf -Dversion=8.31 -Dpackaging=jar
mvn install:install-file -Dfile=jpf-classes.jar -DgroupId=jpf -DartifactId=jpf-classes -Dversion=8.31 -Dpackaging=jar
mvn install:install-file -Dfile=jpf-annotations.jar -DgroupId=jpf -DartifactId=jpf-annotations -Dversion=8.31 -Dpackaging=jar
```
