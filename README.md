# README #

Wrapping up Java sockets for integrated connectivity control

### What is this repository for? ###

* Java Sockets are a great tool. But though the undelying TCP Protocol supports it, timeouts (e.g. as a consequence of router crashes) are not handled out of the box. The usual way of dealing with this, is by sending test probes periodicaly over your established connections and then detect transmission delays / breakdowns.
* However in most cases this is not something you are keen on implementing when developping an application. Good news is, you can use this library to do exactly missing part for you while focusing on the remaining code.

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

### How do I get set up? ###

## Old School ##
* Checkout this repo, integrate included sources (src) or JAR (dist) 

## Maven ##
* Add the following to your pom.xml (somewhere within the ```<project>``` block)

```<repositories>
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
</dependencies>```
