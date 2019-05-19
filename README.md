# Simple Exchange implementation

Current implementation is single-threaded and is very simplistic. It uses heavily Java Streams API
to implement operations.

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