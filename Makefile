classes: .classes.dummy

jar: crow.jar

crow.jar: .classes.dummy
	cd classes; jar cfe ../$@ Main *

deps:; perl build/deps.pl >build/deps.mk

clean:; rm -rf classes/*
realclean: clean
	rm -rf crow.jar

include build/deps.mk
