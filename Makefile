PROJECTDIR=src/netbeans-project
SOOTLIB=$(PROJECTDIR)/lib/sootclasses_j9-trunk-jar-with-dependencies.jar
JAVADIR=/usr/lib/jvm/java-11-openjdk-amd64/

all:
	mkdir -p build
	$(JAVADIR)/bin/javac -d build -cp $(SOOTLIB) $(PROJECTDIR)/src/*/*.java $(PROJECTDIR)/src/*/*/*.java $(PROJECTDIR)/src/*.java
	cp src/scripts/JINN-TOOL.sh build/jinn-c
	chmod +x build/jinn-c

tests:
	mkdir -p build/tests
	$(JAVADIR)/bin/javac -g -d build/tests -cp build $(PROJECTDIR)/../tests/*java

benchs:
	mkdir -p build/benchmarks/bfs
	$(JAVADIR)/bin/javac -g -d build/ -cp build benchmarks/jpbbs/utils/EmbeddedSystem.java
	$(JAVADIR)/bin/javac -g -d build/benchmarks/bfs -cp build:build/benchmarks benchmarks/jpbbs/single/BFS/*.java

clean:
	rm -r build sootOutput runtime-logs soot-out1 soot-out2 preditor-dir | true

