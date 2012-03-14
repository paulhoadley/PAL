# Makefile for PAL Machine Simulator

# PAL Machine Simulator: An implementation in Java
#

JAVAC=		javac
JAVA=		java
JAVADOC=	javadoc
DOCDIR=		html
JAVADOCOPTS=	-version -author -windowtitle "PAL Machine Simulator" \
		-d ${DOCDIR} -private
JAR=		jar
JARFILE=	PAL.jar
JAROPTS=	-cfm

ECHO=		echo

PS2PDF=		/usr/local/ghostscript-6.53/lib/ps2pdf
DVIPS=		/usr/local/bin/dvips
LATEX=		/usr/local/bin/latex
GPIC=		/usr/local/bin/gpic

SRC=		$(wildcard *.java)
SIMPLETESTS=	$(filter-out %.out %.ref test/CVS test/interactive, $(wildcard test/*))
INPUTTESTS=	$(filter-out %.in %.out %.ref test/interactive/CVS, $(wildcard test/interactive/*))


TARBALL=	PAL.tar
TARBALLFILES=	${SRC} $(filter-out %.out test/CVS test/interactive, $(wildcard test/*)) $(filter-out %.out test/interactive/CVS, $(wildcard test/interactive/*)) Makefile COPYRIGHT PAL.pdf

%.class:	%.java
	$(JAVAC) $<

CLASSFILES:=	$(patsubst %.java, %.class, $(shell find . -name '*.java'))

.PHONY:	all
all:		${CLASSFILES}

#Dependencies between classes.
PAL.class:DataStack.class Data.class Code.class Mnemonic.class

DataStack.class:Data.class

stackframe.tex:	stackframe.pic
	${GPIC} -t stackframe.pic > stackframe.tex

PAL.pdf:	PAL.tex stackframe.tex
	${LATEX} PAL.tex
	${LATEX} PAL.tex
	${LATEX} PAL.tex
	${DVIPS} PAL.dvi -Ppdf -o PAL.ps
	${PS2PDF} PAL.ps

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
	rm -f PAL.aux PAL.dvi PAL.lof PAL.log PAL.lot PAL.pdf PAL.ps PAL.toc
	rm -f stackframe.tex

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
tarball:	PAL.pdf
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
