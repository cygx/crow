.classes.dummy: sources/de/cygx/crow/Constants.java sources/Main.java sources/de/cygx/crow/Record.java sources/de/cygx/crow/Repository.java sources/de/cygx/crow/RequestFrame.java sources/de/cygx/crow/Server.java sources/Tree.java sources/de/cygx/crow/Varint.java
	javac -d classes -sourcepath sources $^
	@touch $@

Constants: classes/de/cygx/crow/Constants.class
Main: classes/Main.class
Record: classes/de/cygx/crow/Record.class
Repository: classes/de/cygx/crow/Repository.class
RequestFrame: classes/de/cygx/crow/RequestFrame.class
Server: classes/de/cygx/crow/Server.class
Tree: classes/Tree.class
Varint: classes/de/cygx/crow/Varint.class

classes/de/cygx/crow/Constants.class: sources/de/cygx/crow/Constants.java
	javac -d classes -sourcepath sources $<

classes/Main.class: sources/Main.java sources/de/cygx/crow/Server.java sources/Tree.java
	javac -d classes -sourcepath sources $<

classes/de/cygx/crow/Record.class: sources/de/cygx/crow/Record.java
	javac -d classes -sourcepath sources $<

classes/de/cygx/crow/Repository.class: sources/de/cygx/crow/Repository.java sources/de/cygx/crow/Record.java
	javac -d classes -sourcepath sources $<

classes/de/cygx/crow/RequestFrame.class: sources/de/cygx/crow/RequestFrame.java sources/de/cygx/crow/Constants.java sources/de/cygx/crow/Varint.java
	javac -d classes -sourcepath sources $<

classes/de/cygx/crow/Server.class: sources/de/cygx/crow/Server.java
	javac -d classes -sourcepath sources $<

classes/Tree.class: sources/Tree.java
	javac -d classes -sourcepath sources $<

classes/de/cygx/crow/Varint.class: sources/de/cygx/crow/Varint.java
	javac -d classes -sourcepath sources $<
