# Simple Exchange implementation

This is order exchange simulation. 
Current implementation is single-threaded and uses internally PriorityQueue (which is based on heap data structure).

## How to use

Project uses maven, to build JAR file just use following command:

```
$ mvn verify install
``` 

Afterwards you can add it as dependency to your project:

```
<dependency>
    <groupId>com.simpletrading</groupId>
    <artifactId>exchangetask</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```