# Makefile for PAL Machine Simulator
#
# $Id$

# PAL Machine Simulator: An implementation in Java
#
# Copyright (c) 2002, 2003 Philip J. Roberts and Paul A. Hoadley All
# rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#
# Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#
# Neither the name of the authors nor the names of any other
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

JAVA_HOME=	/usr/local/java
JIKESPATH=	${JAVA_HOME}/jre/lib/rt.jar
JAVAC=		jikes -classpath ${JIKESPATH}
#JAVAC=		javac
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
TARBALLFILES=	${SRC} ${SIMPLETESTS} ${INPUTTESTS} Makefile COPYRIGHT

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
	-@$(foreach test, ${SIMPLETESTS}, ${ECHO} "Testing ${test}"; \
			${JAVA} PAL ${test} > ${test}.out 2>&1; \
			diff ${test}.out ${test}.ref; )

	-@$(foreach test, ${INPUTTESTS}, ${ECHO} "Testing ${test}"; \
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
	-@$(foreach test, ${SIMPLETESTS}, ${JAVA} PAL ${test} > ${test}.ref 2>&1;)
	@${ECHO} "Done."

# Build the .ref files for each of the INPUTTESTS
	@${ECHO} -n "Creating .ref files for interactive tests..."
	-@$(foreach test, ${INPUTTESTS}, ${JAVA} PAL ${test} < ${test}.in > ${test}.ref 2>&1;)
	@${ECHO} "Done."
	@${ECHO} "*** Remember to commit the new *.ref files to CVS. ***"
