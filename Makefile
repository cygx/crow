classes: .classes.dummy

tests: .tests.dummy

jar: crow.jar

html: docs/spec.html

check: run-tests

clean:; rm -rf classes/* tests/*.class tests/*.tmp .*.dummy
realclean: clean
	rm -rf crow.jar docs/*.html

deps:; perl deps.pl >deps.mk

crow.jar: .classes.dummy
	cd classes; jar cfe ../$@ Main *

docs/spec.html: docs/spec.meta spec.md
	multimarkdown --nolabels -o $@ $^
	-@chmod -x $@

include deps.mk
