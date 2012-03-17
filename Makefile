# Makefile for PAL Machine Simulator

# Interesting targets
# * jar: Builds PAL.jar
# * docs: Builds Javadoc API documentation
# * tests: Runs tests
# * clean: Cleans up

# Javadocs
DOCDIR=		html
JAVADOCOPTS=	-version -author -windowtitle "PAL Machine Simulator" \
		-d ${DOCDIR} -private -sourcepath src

# JAR
JARFILE=	PAL.jar
JAROPTS=	-cm -C src -f

# Manual
PS2PDF=		ps2pdf
DVIPS=		dvips
LATEX=		latex
GPIC=		pic

# Tests
SIMPLETESTS=	$(filter-out %.out %.ref, $(wildcard test/basic/*))
INPUTTESTS=	$(filter-out %.in %.out %.ref, $(wildcard test/interactive/*))
JAVA_SOURCE=	$(shell find src -name '*.java')

.PHONY:	compile
compile:
	mkdir -p bin
	javac -d bin ${JAVA_SOURCE}

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
	rm -rf bin
	rm -rf ${DOCDIR}
	rm -f test/basic/*.out
	rm -f test/interactive/*.out
	rm -f ${JARFILE}
	rm -f jar-manifest
	rm -f ${TARBALL}
	rm -f PAL.aux PAL.dvi PAL.lof PAL.log PAL.lot PAL.pdf PAL.ps PAL.toc
	rm -f stackframe.tex

.PHONY: docs
docs:
	mkdir -p ${DOCDIR}
	javadoc ${JAVADOCOPTS} net.logicsquad.pal

.PHONY: jar
jar:	compile
	echo "Main-class: net/logicsquad/pal/PAL" > jar-manifest
	jar cmf jar-manifest ${JARFILE} -C bin net
	rm jar-manifest

.PHONY: test
test:	jar
	@rm -f test/basic/*.out
	@rm -f test/interactive/*.out
	-@$(foreach test, ${SIMPLETESTS}, echo "Testing ${test}"; \
			java -jar PAL.jar ${test} > ${test}.out 2>&1; \
			diff ${test}.out ${test}.ref; )

	-@$(foreach test, ${INPUTTESTS}, echo "Testing ${test}"; \
			java -jar PAL.jar ${test} < ${test}.in > ${test}.out 2>&1; \
			diff ${test}.out ${test}.ref; )
	@echo "Testing complete."

# Re-make the reference for the test files.
.PHONY: test-ref
test-ref:
	@echo -n "Removing old .ref files..."
	@rm -f test/basic/*.ref
	@rm -f test/interactive/*.ref
	@echo "Done."

# Build the .ref files for each of the SIMPLETESTS
	@echo -n "Creating .ref files for simple tests..."
	-@$(foreach test, ${SIMPLETESTS}, java -jar PAL.jar ${test} > ${test}.ref 2>&1;)
	@echo "Done."

# Build the .ref files for each of the INPUTTESTS
	@echo -n "Creating .ref files for interactive tests..."
	-@$(foreach test, ${INPUTTESTS}, java -jar PAL.jar ${test} < ${test}.in > ${test}.ref 2>&1;)
	@echo "Done."
