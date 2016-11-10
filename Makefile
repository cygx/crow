PKG := de/cygx/crow
PKGSOURCES := $(wildcard sources/$(PKG)/*.java)
PKGNAMES := $(patsubst sources/$(PKG)/%.java,%,$(PKGSOURCES))
SOURCES := $(wildcard sources/*.java) $(PKGSOURCES)
CSOURCES := launcher/win32/main.c
CLASSES := $(SOURCES:sources/%.java=classes/%.class)
TESTSOURCES := $(wildcard tests/*.java)
TESTS := $(sort $(patsubst tests/%.java,%,$(TESTSOURCES)))
GARBAGE := launcher/win32/*.o

clean: GARBAGE += .*.dummy classes/* tests/*.class tests/*.tmp
nativeclean: GARBAGE += crow.exe crow-*.exe
realclean: GARBAGE += crow.jar crow.exe crow-*.exe

build: .classes.dummy

check: .tests.dummy
	@set -e; for t in $(TESTS); do echo java -cp "'classes;tests'" -ea $$t; \
		java -cp 'classes;tests' -ea $$t; \
		done

jar: crow.jar

win64: crow.jar crow-w64.exe
	cp crow-w64.exe crow.exe

nativecheck:
	clang -fsyntax-only -Werror -Weverything -Wno-vla $(CSOURCES)

clean nativeclean realclean:; rm -rf $(GARBAGE)

crow Tree: %: classes/%.class
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

launcher/win32/main-w64.o: launcher/win32/main.c
	$(CC) -c -O3 -o $@ $<

launcher/win32/res-w64.o: launcher/win32/res.rc launcher/win32/glider.ico
	$(WINDRES) -o $@ $<

crow-w64.exe: CC := x86_64-w64-mingw32-gcc
crow-w64.exe: WINDRES := x86_64-w64-mingw32-windres -F pe-x86-64
crow-w64.exe: crow-%.exe: launcher/win32/main-%.o launcher/win32/res-%.o
	$(CC) -s -o $@ $^
