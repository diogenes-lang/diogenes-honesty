# Installation for users #

See [http://co2.unica.it/diogenes](http://co2.unica.it/diogenes)

-----------------------------------------------------------------

# Installation for developers #

## Java PathFinder
Clone the repository of JPF. See the official website for the building instructions.
You must obtain three jars: `jpf.jar`,`jpf-classes.jar` and `jpf-annotations.jar`

## Maven

Create JPF packages to use it within maven. Set the version according to JPF version/commit (only for coherence).
```
mvn install:install-file -Dfile=jpf.jar -DgroupId=jpf -DartifactId=jpf -Dversion=8-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=jpf-classes.jar -DgroupId=jpf -DartifactId=jpf-classes -Dversion=8-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=jpf-annotations.jar -DgroupId=jpf -DartifactId=jpf-annotations -Dversion=8-SNAPSHOT -Dpackaging=jar
```
Now you can import `co2-honesty-checker` in your IDE which supports Maven.

Download [co2-api.jar](http://co2.unica.it/downloads/co2api/co2api-0.0.9.jar) and install it locally

```
mvn install:install-file -Dfile=co2api-0.0.9.jar -DgroupId=tcs.unica.it -DartifactId=co2api -Dversion=0.0.9 -Dpackaging=jar
```

## Build ##

```
mvn clean compile assembly:single
```


## Configuration ##
Create the file `/co2-honesty-checker/src/main/resources/local.properties` and set the following properties

* `honesty.maude.co2-maude` : the absolute path to the co2 maude files that check the honesty
* `honesty.maude.exec` : the absolute path to the maude executable
