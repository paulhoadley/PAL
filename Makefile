# Makefile for PAL machine project
#
# $Id$

JAVA_HOME=	/usr/local/java
JIKESPATH=	${JAVA_HOME}/jre/lib/rt.jar
JAVAC=		jikes -classpath ${JIKESPATH}
JAVADOC=	${JAVA_HOME}/bin/javadoc
DOCDIR=		html
JAVADOCOPTS=	-version -author -windowtitle "PAL Machine Simulator" \
		-d ${DOCDIR} -private

SRC=		$(wildcard *.java)

%.class:	%.java
	$(JAVAC) $<

CLASSFILES:=	$(patsubst %.java, %.class, $(shell find . -name '*.java'))

.PHONY:	all
all:		${CLASSFILES}

.PHONY: clean
clean:
	rm -f *~
	rm -f *.class
	rm -r ${DOCDIR}

.PHONY: docs
docs:
	mkdir -p ${DOCDIR}
	${JAVADOC} ${JAVADOCOPTS} ${SRC}
