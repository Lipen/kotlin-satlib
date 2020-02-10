BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)/classes
HEADERS_DIR = $(BUILD_DIR)/headers

JMINISAT_CLASSPATH = $(CLASSES_DIR)/kotlin/main
JMINISAT_CLASSNAME = com.github.lipen.jnisat.JMiniSat
JMINISAT_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JMINISAT_CLASSNAME)).h
JMINISAT_SRC = src/main/cpp/JMiniSat.cpp
JMINISAT_LIB = src/main/resources/lib/linux64/libjminisat.so

JAVA_HOME ?= $(shell readlink -f /usr/bin/javac | sed "s:/bin/javac::")
JAVA_INCLUDE = $(JAVA_HOME)/include

CC = g++
CCFLAGS = -Wall -O3 -fPIC -fpermissive -shared -s
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS =
LDLIBS = -lminisat

.PHONY: default libjminisat headers classes

default:
	@echo 'Specify a target! [all libjminisat headers classes]'

all: classes headers libjminisat

libjminisat $(JMINISAT_LIB): $(JMINISAT_HEADER)
	@echo "=== Building libjminisat..."
	@mkdir -p $(shell dirname $(JMINISAT_LIB))
	$(CC) -o $(JMINISAT_LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(LDLIBS) $(JMINISAT_SRC)

headers $(JMINISAT_HEADER): $(CLASSES_DIR)
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(JMINISAT_CLASSPATH) $(JMINISAT_CLASSNAME)

classes $(CLASSES_DIR):
	@echo "=== Compiling classes..."
	./gradlew -q classes
