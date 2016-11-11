Constants: classes/de/cygx/crow/Constants.class
Main: classes/Main.class
Record: classes/de/cygx/crow/Record.class
Repository: classes/de/cygx/crow/Repository.class
RequestFrame: classes/de/cygx/crow/RequestFrame.class
Server: classes/de/cygx/crow/Server.class
Test: classes/Test.class
Tree: classes/Tree.class
Varint: classes/de/cygx/crow/Varint.class

.classes.dummy: sources/de/cygx/crow/Constants.java sources/Main.java sources/de/cygx/crow/Record.java sources/de/cygx/crow/Repository.java sources/de/cygx/crow/RequestFrame.java sources/de/cygx/crow/Server.java sources/Test.java sources/Tree.java sources/de/cygx/crow/Varint.java
	javac -d classes -sourcepath sources $^
	@touch $@

classes/Main.class: sources/de/cygx/crow/Repository.java sources/de/cygx/crow/Server.java sources/Tree.java
classes/de/cygx/crow/Repository.class: sources/de/cygx/crow/Record.java
classes/de/cygx/crow/RequestFrame.class: sources/de/cygx/crow/Constants.java sources/de/cygx/crow/Varint.java

classes/de/cygx/crow/Constants.class classes/Main.class classes/de/cygx/crow/Record.class classes/de/cygx/crow/Repository.class classes/de/cygx/crow/RequestFrame.class classes/de/cygx/crow/Server.class classes/Test.class classes/Tree.class classes/de/cygx/crow/Varint.class: classes/%.class: sources/%.java
	javac -d classes -sourcepath sources $<

run-tests: .tests.dummy
	java -cp 'classes;tests' -ea t01
	java -cp 'classes;tests' -ea t02

t01 t02: %: tests/%.class .classes.dummy
	java -cp 'classes;tests' -ea $@

.tests.dummy: .classes.dummy tests/t01.java tests/t02.java
	javac -cp classes $(filter %.java,$^)
	@touch $@

tests/t01.class tests/t02.class: %.class: %.java .classes.dummy
	javac -cp 'classes;tests' $<
