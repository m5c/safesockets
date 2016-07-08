# README #

## About ##

### In one sentence ###

Wrapping up Java sockets for integrated connectivity monitoring 

### A little more & motivation ###

* Java Sockets are a great tool. But though the underlying TCP Protocol supports such, timeouts (e.g. as a consequence of router crashes) are not easily configurable out of the box. 
* This is usualy dealt with by adding extra functionality to the application layer. That is to say code sending test probes and ACKs repeatedly over established connections. This way you can detect connectivity issues in (almost) real time.
* However in most cases this is not something you are keen on implementing when developping an application. Good news is, you can use this library to do exactly that missing part for you, while focusing on the actual application code.

## Features ##

### Pro ###

* Both sides detect connection breakdowns automatically
* Offers interfaces for your own Observers (Breakdown and Incoming Message)
* You can use you own params for timeouts, sending-probe-intervals, ...
* Compiled against JDK1.6, so you can use it on Android (though by no means a replacement for PNS)
* Checks message integrity by using hashes
* Blocks on sending until reception of ACK, so you can rely on your message having been transmitted
* Easy to use
* No dependencies

### Con ###

* Not Thread safe (Does not support concurrent sending of several messages on one single SafeSocket's end)
* Currently only Strings supported (as message type).

## How do I get set up? ##

### Old School ###
* Checkout this repo, integrate included sources (src) or JAR (target) 

### Maven ###
* Add the following to your pom.xml (somewhere within the ```<project>``` block)

```
<repositories>
	[...]
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
	[...]
</repositories>

[...]

<dependencies>
	[...]
	<dependency>
		<groupId>com.github.m5c</groupId>
		<artifactId>safesockets</artifactId>
		<version>-SNAPSHOT</version>
	</dependency>
	[...]
</dependencies>
```
