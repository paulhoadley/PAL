# Makefile for PAL machine project
#
# $Id$

JAVA_HOME=	/usr/local/java
JIKESPATH=	${JAVA_HOME}/jre/lib/rt.jar
JAVAC=		jikes -classpath ${JIKESPATH}
JAVA=		${JAVA_HOME}/bin/java
JAVADOC=	${JAVA_HOME}/bin/javadoc
DOCDIR=		html
JAVADOCOPTS=	-version -author -windowtitle "PAL Machine Simulator" \
		-d ${DOCDIR} -private

SRC=		$(wildcard *.java)
TESTFILES=	$(filter-out test/CVS, $(wildcard test/*))

%.class:	%.java
	$(JAVAC) $<

CLASSFILES:=	$(patsubst %.java, %.class, $(shell find . -name '*.java'))

.PHONY:	all
all:		${CLASSFILES}

#Dependencies between classes.
PAL.class:DataStack.class Data.class Code.class Mnemonic.class

DataStack.class:Data.class

.PHONY: clean
clean:
	rm -f *~
	rm -f *.class
	rm -rf ${DOCDIR}
	rm -f test.out

.PHONY: docs
docs:
	mkdir -p ${DOCDIR}
	${JAVADOC} ${JAVADOCOPTS} ${SRC}

.PHONY: test
test:
	rm -f test.out
	@$(foreach test, ${TESTFILES}, ${JAVA} PAL ${test} >> test.out 2>&1;)
	diff test.out test.ref

# Re-make the reference for the test files.
.PHONY: test-ref
test-ref:
	rm -f test.ref
	$(foreach test, ${TESTFILES}, ${JAVA} PAL ${test} >> test.ref 2>&1;)
