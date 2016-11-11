classes: .classes.dummy

tests: .tests.dummy

jar: crow.jar

deps:; perl deps.pl >deps.mk

clean:; rm -rf classes/* tests/*.class tests/*.tmp .*.dummy
realclean: clean
	rm -rf crow.jar

crow.jar: .classes.dummy
	cd classes; jar cfe ../$@ Main *

check: run-tests

include deps.mk
