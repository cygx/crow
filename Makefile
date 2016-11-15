MMDFLAGS := -f

classes: .classes.dummy

tests: .tests.dummy

jar: crow.jar

html: docs/spec.html

latex: MMDFLAGS += -t latex
latex: docs/spec.tex

check: run-tests

clean:; rm -rf classes/* tests/*.class tests/*.tmp .*.dummy
realclean: clean
	rm -rf crow.jar docs/*.html

deps:; perl deps.pl >deps.mk

crow.jar: .classes.dummy
	cd classes; jar cfe ../$@ Main *

docs/spec.html docs/spec.tex: docs/spec.%: docs/spec-%.meta spec.md
	multimarkdown $(MMDFLAGS) -o $@ $^
	-@chmod -x $@

.classes.dummy .tests.dummy: deps.mk

prove prove-v: MAKEFLAGS += --no-print-directory

include deps.mk
