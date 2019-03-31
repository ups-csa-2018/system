.PHONY: clean
JAVA_FILES=$(wildcard *.java)
TARGETS=$(JAVA_FILES:.java=.class)

all: $(TARGETS)

%.class : %.java
	javac $<

clean:
	rm -f *.class
