# Makefile for PAL machine project

JAVA_HOME=	/usr/local/java
JIKESPATH=	${JAVA_HOME}/jre/lib/rt.jar
JAVAC=		jikes -classpath ${JIKESPATH}

%.class:	%.java
	$(JAVAC) $<

CLASSFILES:=	$(patsubst %.java, %.class, $(shell find . -name '*.java'))

.PHONY:	all
all:		${CLASSFILES}
