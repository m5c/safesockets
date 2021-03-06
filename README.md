# README #

## About ##

### In one sentence ###

Wrapping up Java sockets for integrated connectivity monitoring 

### A little more & motivation ###

* Java Sockets are a great tool. But though the underlying TCP Protocol supports such, timeouts (e.g. as a consequence of router crashes) are not easily configurable out of the box. 
* This is usually dealt with by adding extra functionality to the application layer. That is to say code sending test probes and ACKs repeatedly over established connections. This way you can detect connectivity issues in (almost) real time.
* However in most cases this is not something you are keen on implementing when developing an application. Good news is, you can use this library to do exactly that missing part for you, while focusing on the actual application code.

## Features ##

### Pro ###

* Both sides detect connection breakdowns automatically
* Offers interfaces for your own Observers (Breakdown and Incoming Message)
* You can use you own params for timeouts, sending-probe-intervals, ...
* Compiled against JDK 1.7, so you can use it on Android (though by no means a replacement for traditional PNS)
* Checks message integrity by using hashes
* Blocks on sending until reception of ACK (or timeout) and returns you a boolean, so you know when a message has been transmitted for sure. 
* Supports concurrent senders. You can have multiple threads sending messages through one and the same SafeSocket. The messages will still be identified and ACKed individually on the other side.
* Easy to use
* No dependencies

### Con ###

* Currently only Strings supported (as message type).
* In the rare case of a connection breakdown during the sending of a message ACK, you will be returned "false" on the sending method, though the message has actually been transferred. But anyways you will still be notified about the connection breakdown itself.

## How do I get started? ##

### Old School ###
* Checkout this repo
* Integrate the included sources (src) or JAR (target) into your project

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

### Gradle ###
* Add this to your root build.gradle (the one at top level in your project)
```
allprojects {
    repositories {
    	jcenter()
	maven { url "https://jitpack.io" }
    }
}
```

* Then add this to the build.gradle at app scope (usualy one level down to the previous)
```
dependencies {
    compile 'com.github.m5c:safesockets:-SNAPSHOT'
}
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
* Hint 1: serverSocket is a classic Socket. This one will be used internally. Managing the reference outside the SafeSocket wrapper allows the creation of multiple SafeSockets on a single port (at least for connection establishment).
* Hint 2: The constructor blocks until a client has connected.
* Hint 3: You can run multiple SafeSockets on the same port by reusing the same serverSocket entity.

### Client side setup ###
Connect to a server by specifying IP and port instead of the ServerSocket:
``` Java
new SafeSocket("192.168.1.42", 2610, HEART_BEAT_RATE, TIMEOUT, messageObservers, breakdownObservers);
```
* Note: HEART_BEAT_RATE and TIMEOUT must be identical to the parameters on server side.

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

Note: As soon as a connection is considered lost, you cannot send any further messages. Just discard it and create a new one.
