PKG := de/cygx/crow
PKGSOURCES := $(wildcard java/$(PKG)/*.java)
PKGNAMES := $(patsubst java/$(PKG)/%.java,%,$(PKGSOURCES))
SOURCES := java/crow.java $(PKGSOURCES)
CLASSES := $(SOURCES:java/%.java=class/%.class)
TESTSOURCES := $(wildcard test/*.java)
TESTS := $(sort $(patsubst test/%.java,%,$(TESTSOURCES)))
GARBAGE := .*.dummy class/* test/*.class

build: .class.dummy

check: .test.dummy
	@set -e; for t in $(TESTS); do echo java -cp "'class;test'" -ea $$t; \
		java -cp 'class;test' -ea $$t; \
		done

jar: crow.jar

realclean: GARBAGE += crow.jar
clean realclean:; rm -rf $(GARBAGE)

crow: class/crow.class
$(PKGNAMES): %: class/$(PKG)/%.class

.class.dummy: $(SOURCES)
	javac -d class $(SOURCES)
	touch $@

.test.dummy: .class.dummy $(TESTSOURCES)
	javac -cp class $(TESTSOURCES)
	touch $@

$(CLASSES): class/%.class: java/%.java $(SOURCES)
	javac -sourcepath java -d class $<

crow.jar: .class.dummy
	cd class; jar cfe ../$@ crow *
