# Makefile for PAL Machine Simulator

# Interesting targets
# * jar: Builds PAL.jar
# * docs: Builds Javadoc API documentation
# * tests: Runs tests
# * clean: Cleans up
# * manual: Builds a PDF manual in the doc subdirectory (requires LaTeX)

# Javadocs
DOCDIR=		html
JAVADOCOPTS=	-version -author -windowtitle "PAL Machine Simulator" \
		-d ${DOCDIR} -private -sourcepath src

# JAR
JARFILE=	PAL.jar
JAROPTS=	-cm -C src -f

# Tests
SIMPLETESTS=	$(filter-out %.out %.ref, $(wildcard test/basic/*))
INPUTTESTS=	$(filter-out %.in %.out %.ref, $(wildcard test/interactive/*))
JAVA_SOURCE=	$(shell find src -name '*.java')

.PHONY:	compile
compile:
	mkdir -p bin
	javac -d bin ${JAVA_SOURCE}

.PHONY: clean
clean:
	rm -rf bin
	rm -rf ${DOCDIR}
	rm -f test/basic/*.out
	rm -f test/interactive/*.out
	rm -f ${JARFILE}
	rm -f jar-manifest
	rm -f ${TARBALL}
	make -C doc clean

.PHONY: docs
docs:
	mkdir -p ${DOCDIR}
	javadoc ${JAVADOCOPTS} net.logicsquad.pal

.PHONY:	manual
manual:
	make -C doc PAL.pdf

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
