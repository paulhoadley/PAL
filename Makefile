# Makefile for PAL machine project
#
# Authors: Paul Hoadley <paulh@logicsquad.net>
#          Philip Roberts <philip.roberts@student.adelaide.edu.au>
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
JAR=		${JAVA_HOME}/bin/jar
JARFILE=	PAL.jar
JAROPTS=	-cfm

ECHO=		/usr/ucb/echo

SRC=		$(wildcard *.java)
SIMPLETESTS=	$(filter-out %.out %.ref test/CVS test/interactive, $(wildcard test/*))
INPUTTESTS=	$(filter-out %.in %.out %.ref test/interactive/CVS, $(wildcard test/interactive/*))

TARBALL=	PAL.tar
TARBALLFILES=	${SRC} ${SIMPLETESTS} ${INPUTTESTS} Makefile

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
	rm -f test/*.out
	rm -f test/interactive/*.out
	rm -f ${JARFILE}
	rm -f jar-manifest
	rm -f ${TARBALL}

.PHONY: docs
docs:
	mkdir -p ${DOCDIR}
	${JAVADOC} ${JAVADOCOPTS} ${SRC}

.PHONY: jar
jar:	${CLASSFILES}
	echo "Main-class: PAL" > jar-manifest
	${JAR} ${JAROPTS} ${JARFILE} jar-manifest ${CLASSFILES}
	rm jar-manifest

.PHONY: tarball
tarball:
	tar -cvf ${TARBALL} ${TARBALLFILES}

.PHONY: test
test:
	@rm -f test/*.out
	@$(foreach test, ${SIMPLETESTS}, ${ECHO} "Testing ${test}"; \
			${JAVA} PAL ${test} > ${test}.out 2>&1; \
			diff ${test}.out ${test}.ref; )

	@$(foreach test, ${INPUTTESTS}, ${ECHO} "Testing ${test}"; \
			${JAVA} PAL ${test} < ${test}.in > ${test}.out 2>&1; \
			diff ${test}.out ${test}.ref; )
	@${ECHO} "Testing complete."

# Re-make the reference for the test files.
.PHONY: test-ref
test-ref:
	@${ECHO} -n "Removing old .ref files..."
	@rm -f test/*.ref
	@rm -f test/interactive/*.ref
	@${ECHO} "Done."

# Build the .ref files for each of the SIMPLETESTS
	@${ECHO} -n "Creating .ref files for simple tests..."
	@$(foreach test, ${SIMPLETESTS}, ${JAVA} PAL ${test} > ${test}.ref 2>&1;)
	@${ECHO} "Done."

# Build the .ref files for each of the INPUTTESTS
	@${ECHO} -n "Creating .ref files for interactive tests..."
	@$(foreach test, ${INPUTTESTS}, ${JAVA} PAL ${test} < ${test}.in > ${test}.ref 2>&1;)
	@${ECHO} "Done."
	@${ECHO} "*** Remember to commit the new *.ref files to CVS. ***"
