.PHONY: clean tests test1 test2 test3

JAVA_FILES=$(wildcard *.java)
TARGETS=$(JAVA_FILES:.java=.class)

all: $(TARGETS)

%.class : %.java
	javac $<

test1: Main.class tests/input1 tests/output1
	java Main tests/input1 | diff tests/output1 -
	@echo "test 1 passed"

test2: Main.class tests/input2 tests/output2
	java Main tests/input2 | diff tests/output2 -
	@echo "test 2 passed"

test3: Main.class tests/input3 tests/output3
	java Main tests/input3 | diff tests/output3 -
	@echo "test 3 passed"

test4: Main.class tests/input4 tests/output4
	java Main tests/input4 | diff tests/output4 -
	@echo "test 4 passed"

tests: test1 test2 test3 test4
	@echo "all tests passed"

clean:
	rm -f *.class
