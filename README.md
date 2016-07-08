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
* Supports concurrent sending. You can have multiple threads sending messages through one and the same SafeSocket. The will still be received and ACKed individually.
* Easy to use
* No dependencies

### Con ###

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

## How do I use SafeSockets? ##

### Params ###
``` Java
int HEART_BEAT_RATE = 2000;
```
as the time interval between two probes [ms].

``` Java
int TIMEOUT = 500;
```
as the max delay [ms] for an ACK before the connection is considered lost.

### Server side setup ###
Create a SafeSocket by using the following constructor:
``` Java
new SafeSocket(serverSocket, HEART_BEAT_RATE, TIMEOUT, messageObservers, breakDownObservers);
```
The Observers are Collections of the provided observer interfaces. May be empty but not null.
(Hint 1: The contructor blocks until a client has connected.)
(Hint 2: You can run multiple SafeSockets on the same port by reusing the same serverSocket entity.)

### Client side setup ###
Connect to a server by specifying IP and port instead of the ServerSocket:
``` Java
new SafeSocket("192.168.1.42", 2610, HEART_BEAT_RATE, TIMEOUT, messageObservers, breakdownObservers);
```
Note: HEART_BEAT_RATE and TIMEOUT must be identical to the parameters on server side.

### Send some actual content ###
You can send strings by calling:
``` Java
safeSocket.sendMessage("Foo");
```
This instruction will block and return either
* true:		if the remote client has ACKed the message before it's timeout
* false:	if a timeout occured while waiting for the ACK

### Events ###
You can register your own observers by passing them (in collections) to the SafeSocket constructor.
* MessageObservers: Will be notified on each incoming message (except for probes & acks)
* BreakDownObservers: Will be notified as soon as the connection is considered lost.

Note: As soon as a connection is considered lost, you will cannot send any further messages. Just discard it and create a new one.
