PKG := de/cygx/crow
PKGSOURCES := $(wildcard sources/$(PKG)/*.java)
PKGNAMES := $(patsubst sources/$(PKG)/%.java,%,$(PKGSOURCES))
SOURCES := sources/crow.java $(PKGSOURCES)
CLASSES := $(SOURCES:sources/%.java=classes/%.class)
TESTSOURCES := $(wildcard tests/*.java)
TESTS := $(sort $(patsubst tests/%.java,%,$(TESTSOURCES)))
GARBAGE := .*.dummy classes/* tests/*.class tests/*.tmp

build: .classes.dummy

check: .tests.dummy
	@set -e; for t in $(TESTS); do echo java -cp "'classes;tests'" -ea $$t; \
		java -cp 'classes;tests' -ea $$t; \
		done

jar: crow.jar

realclean: GARBAGE += crow.jar
clean realclean:; rm -rf $(GARBAGE)

crow: classes/crow.class
$(PKGNAMES): %: classes/$(PKG)/%.class

$(TESTS): %: tests/%.class
	java -cp 'classes;tests' -ea $@

tests/%.class: tests/%.java .classes.dummy
	javac -cp classes $<

.classes.dummy: $(SOURCES)
	javac -d classes $(SOURCES)
	@touch $@

.tests.dummy: .classes.dummy $(TESTSOURCES)
	javac -cp classes $(TESTSOURCES)
	@touch $@

$(CLASSES): classes/%.class: sources/%.java $(SOURCES)
	javac -sourcepath sources -d classes $<

crow.jar: .classes.dummy
	cd classes; jar cfe ../$@ crow *
