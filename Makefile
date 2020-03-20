BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)/classes
HEADERS_DIR = $(BUILD_DIR)/headers
SRC_DIR = src/main
CPP_DIR = $(SRC_DIR)/cpp
LIB_DIR = $(BUILD_DIR)/lib
LIB_RES = $(SRC_DIR)/resources/lib/linux64

CLASSPATH = $(CLASSES_DIR)/kotlin/main
PACKAGE = com.github.lipen.jnisat

JMINISAT_CLASSNAME = $(PACKAGE).JMiniSat
JMINISAT_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JMINISAT_CLASSNAME)).h
JMINISAT_SRC = $(CPP_DIR)/JMiniSat.cpp
JMINISAT_LIBNAME = libjminisat.so
JMINISAT_LIB = $(LIB_DIR)/$(JMINISAT_LIBNAME)
JMINISAT_RES = $(LIB_RES)/$(JMINISAT_LIBNAME)
MINISAT_INCLUDE_DIR ?= /usr/local/include
MINISAT_LIB_DIR ?= /usr/local/lib
MINISAT_CPPFLAGS = -I$(MINISAT_INCLUDE_DIR)
MINISAT_LDFLAGS = -L$(MINISAT_LIB_DIR) -lminisat

JCADICAL_CLASSNAME = $(PACKAGE).JCadical
JCADICAL_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JCADICAL_CLASSNAME)).h
JCADICAL_SRC = $(CPP_DIR)/JCadical.cpp
JCADICAL_LIBNAME = libjcadical.so
JCADICAL_LIB = $(LIB_DIR)/$(JCADICAL_LIBNAME)
JCADICAL_RES = $(LIB_RES)/$(JCADICAL_LIBNAME)
CADICAL_INCLUDE_DIR ?= /usr/local/include
CADICAL_LIB_DIR ?= /usr/local/lib
CADICAL_CPPFLAGS = -I$(CADICAL_INCLUDE_DIR)
CADICAL_LDFLAGS = -L$(CADICAL_LIB_DIR) -lcadical

CLASSNAMES = $(JMINISAT_CLASSNAME) $(JCADICAL_CLASSNAME)
HEADERS = $(JMINISAT_HEADER) $(JCADICAL_HEADER)
LIBS = $(JMINISAT_LIB) $(JCADICAL_LIB)

JAVA_HOME ?= $(subst /bin/javac,,$(realpath /usr/bin/javac))
JAVA_INCLUDE = $(JAVA_HOME)/include

DOCKER_IMAGE_NAME ?= kotlin-jnisat-builder
DOCKER_PROJECT_DIR ?= /kotlin-jnisat

CC ?= g++
CCFLAGS = -Wall -O3 -fPIC -fpermissive
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS = -shared -s

.PHONY: default libjminisat libjcadical libs headers classes res clean vars

default:
	@echo "Specify a target! [all libs libs-docker libjminisat libjcadical headers classes res clean vars]"
	@echo " - libs -- Build all libraries"
	@echo " - libs-docker -- Build all libraries using Docker"
	@echo " - libjminisat -- Build jminisat library"
	@echo " - libjcadical -- Build jcadical library"
	@echo " - headers -- Generate JNI headers from classes via javah"
	@echo " - classes -- Compile Java/Kotlin classes (run 'gradlew classes')"
	@echo " - res -- Copy libraries to '$(LIB_RES)'"
	@echo " - clean -- Run 'gradlew clean'"
	@echo " - vars -- Show Makefile variables"

all: headers libs res
libs: libjminisat libjcadical

libs-docker: $(HEADERS)
	@echo "=== Building libs in Docker..."
	docker build --tag $(DOCKER_IMAGE_NAME) --build-arg PROJECT_DIR=$(DOCKER_PROJECT_DIR) .
	{ \
		set -e ;\
		docker inspect $(DOCKER_IMAGE_NAME) >/dev/null 2>&1 || \
			echo "Docker image '$(DOCKER_IMAGE_NAME)' does not exist!" ;\
		id=$$(docker create $(DOCKER_IMAGE_NAME)) ;\
		docker cp $${id}:$(DOCKER_PROJECT_DIR)/$(LIB_DIR)/. $(LIB_DIR) ;\
		docker rm --volumes $${id} ;\
	}

libjminisat: LIB = $(JMINISAT_LIB)
libjminisat: SRC = $(JMINISAT_SRC)
libjminisat: CPPFLAGS += $(MINISAT_CPPFLAGS)
libjminisat: LDFLAGS += $(MINISAT_LDFLAGS)
libjminisat $(JMINISAT_LIB):
	@echo "=== Building libjminisat library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjcadical: LIB = $(JCADICAL_LIB)
libjcadical: SRC = $(JCADICAL_SRC)
libjcadical: CPPFLAGS += $(CADICAL_CPPFLAGS)
libjcadical: LDFLAGS += $(CADICAL_LDFLAGS)
libjcadical $(JCADICAL_LIB):
	@echo "=== Building libjcadical library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjminisat libjminisat: $(LIB_DIR)

$(LIB_DIR):
	@echo "=== Creating libdir..."
	mkdir -p $@

headers $(HEADERS): $(CLASSES_DIR)
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(CLASSPATH) $(CLASSNAMES)

classes $(CLASSES_DIR):
	@echo "=== Compiling classes..."
	./gradlew classes

res: $(LIBS)
	@echo "=== Copying libraries to resources..."
	install -d $(dir $(JMINISAT_RES))
	install -m 644 $(JMINISAT_LIB) $(JMINISAT_RES)
	install -m 644 $(JCADICAL_LIB) $(JCADICAL_RES)

clean:
	@echo "=== Cleaning..."
	./gradlew clean

vars:
	$(foreach v, $(sort $(.VARIABLES)), $(if $(filter file,$(origin $(v))), $(info $(v)=$($(v)))))
