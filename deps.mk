DecodingException: classes/de/cygx/crow/DecodingException.class
EncodingException: classes/de/cygx/crow/EncodingException.class
Main: classes/Main.class
Record: classes/de/cygx/crow/Record.class
Repository: classes/de/cygx/crow/Repository.class
RequestFrameBuilder: classes/de/cygx/crow/RequestFrameBuilder.class
RequestFrameReader: classes/de/cygx/crow/RequestFrameReader.class
RequestType: classes/de/cygx/crow/RequestType.class
Server: classes/de/cygx/crow/Server.class
Test: classes/Test.class
Tree: classes/Tree.class
Varint: classes/de/cygx/crow/Varint.class

.classes.dummy: sources/de/cygx/crow/DecodingException.java sources/de/cygx/crow/EncodingException.java sources/Main.java sources/de/cygx/crow/Record.java sources/de/cygx/crow/Repository.java sources/de/cygx/crow/RequestFrameBuilder.java sources/de/cygx/crow/RequestFrameReader.java sources/de/cygx/crow/RequestType.java sources/de/cygx/crow/Server.java sources/Test.java sources/Tree.java sources/de/cygx/crow/Varint.java
	javac -d classes -sourcepath sources $(filter %.java,$^)
	@touch $@

classes/Main.class: sources/de/cygx/crow/Repository.java sources/de/cygx/crow/Server.java sources/Tree.java
classes/de/cygx/crow/Repository.class: sources/de/cygx/crow/Record.java
classes/de/cygx/crow/RequestFrameBuilder.class: sources/de/cygx/crow/RequestType.java

classes/de/cygx/crow/DecodingException.class classes/de/cygx/crow/EncodingException.class classes/Main.class classes/de/cygx/crow/Record.class classes/de/cygx/crow/Repository.class classes/de/cygx/crow/RequestFrameBuilder.class classes/de/cygx/crow/RequestFrameReader.class classes/de/cygx/crow/RequestType.class classes/de/cygx/crow/Server.class classes/Test.class classes/Tree.class classes/de/cygx/crow/Varint.class: classes/%.class: sources/%.java
	javac -d classes -sourcepath sources $<

prove: .tests.dummy
	prove -e$(MAKE) t01 t02

prove-v: .tests.dummy
	prove -v -e$(MAKE) t01 t02

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
