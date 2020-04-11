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

JCRYPTOMINISAT_CLASSNAME = $(PACKAGE).JCryptominisat
JCRYPTOMINISAT_HEADER = $(HEADERS_DIR)/$(subst .,_,$(JCRYPTOMINISAT_CLASSNAME)).h
JCRYPTOMINISAT_SRC = $(CPP_DIR)/JCryptominisat.cpp
JCRYPTOMINISAT_LIBNAME = libjcryptominisat.so
JCRYPTOMINISAT_LIB = $(LIB_DIR)/$(JCRYPTOMINISAT_LIBNAME)
JCRYPTOMINISAT_RES = $(LIB_RES)/$(JCRYPTOMINISAT_LIBNAME)
CRYPTOMINISAT_INCLUDE_DIR ?= /usr/local/include
CRYPTOMINISAT_LIB_DIR ?= /usr/local/lib
CRYPTOMINISAT_CPPFLAGS = -I$(CRYPTOMINISAT_INCLUDE_DIR)
CRYPTOMINISAT_LDFLAGS = -Wl,-rpath,$(CRYPTOMINISAT_LIB_DIR)-L$(CRYPTOMINISAT_LIB_DIR) -lcryptominisat5

CLASSNAMES = $(JMINISAT_CLASSNAME) $(JCADICAL_CLASSNAME) $(JCRYPTOMINISAT_CLASSNAME)
HEADERS = $(JMINISAT_HEADER) $(JCADICAL_HEADER) $(JCRYPTOMINISAT_HEADER)
LIBS = $(JMINISAT_LIB) $(JCADICAL_LIB) $(JCRYPTOMINISAT_LIB)

JAVA_HOME ?= $(subst /bin/javac,,$(realpath /usr/bin/javac))
JAVA_INCLUDE = $(JAVA_HOME)/include

DOCKER_IMAGE_NAME ?= kotlin-jnisat-builder
DOCKER_PROJECT_DIR ?= /kotlin-jnisat
DOCKER_LIB_DIR = $(DOCKER_PROJECT_LIB)/$(LIB_DIR)
DOCKER_MINISAT_DIR ?= /minisat
DOCKER_CADICAL_DIR ?= /cadical
DOCKER_CRYPTOMINISAT_DIR ?= /cryptominisat

CC ?= g++
CCFLAGS = -Wall -O3 -fPIC -fpermissive
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS = -shared -s

.PHONY: default libjminisat libjcadical libjcryptominisat libs headers classes res clean vars

default:
	@echo "Specify a target! [all libs libs-docker libjminisat libjcadical headers classes res clean vars]"
	@echo " - libs -- Build all libraries"
	@echo " - libs-docker -- Build all libraries using Docker"
	@echo " - libjminisat -- Build jminisat library"
	@echo " - libjcadical -- Build jcadical library"
	@echo " - libjcryptominisat -- Build jcryptominisat library"
	@echo " - headers -- Generate JNI headers from classes via javah"
	@echo " - classes -- Compile Java/Kotlin classes (run 'gradlew classes')"
	@echo " - res -- Copy libraries to '$(LIB_RES)'"
	@echo " - clean -- Run 'gradlew clean'"
	@echo " - vars -- Show Makefile variables"

all: headers libs res
libs: libjminisat libjcadical libjcryptominisat

libs-docker: $(HEADERS) $(LIB_DIR)
	@echo "=== Building libs in Docker..."
	docker build --tag $(DOCKER_IMAGE_NAME) \
		--build-arg PROJECT_DIR=$(DOCKER_PROJECT_DIR) \
		--build-arg MINISAT_DIR=$(DOCKER_MINISAT_DIR) \
		--build-arg CADICAL_DIR=$(DOCKER_CADICAL_DIR) \
		--build-arg CRYPTOMINISAT_DIR=$(DOCKER_CRYPTOMINISAT_DIR) \
	.
	{ \
		set -e ;\
		docker inspect $(DOCKER_IMAGE_NAME) >/dev/null 2>&1 || \
			echo "Docker image '$(DOCKER_IMAGE_NAME)' does not exist!" ;\
		id=$$(docker create $(DOCKER_IMAGE_NAME)) ;\
		docker cp $${id}:$(DOCKER_PROJECT_DIR)/$(LIB_DIR)/. $(LIB_DIR)/ ;\
		docker cp -L $${id}:/usr/local/lib/libminisat.so $(LIB_DIR)/ ;\
		docker cp -L $${id}:/usr/local/lib/libcadical.so $(LIB_DIR)/ ;\
		docker cp -L $${id}:/usr/local/lib/libcryptominisat5.so $(LIB_DIR)/ ;\
		docker rm --volumes $${id} ;\
	}

libjminisat: LIB = $(JMINISAT_LIB)
libjminisat: SRC = $(JMINISAT_SRC)
libjminisat: CPPFLAGS += $(MINISAT_CPPFLAGS)
libjminisat: LDFLAGS += $(MINISAT_LDFLAGS)
libjminisat $(JMINISAT_LIB): $(LIB_DIR)
	@echo "=== Building libjminisat library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjcadical: LIB = $(JCADICAL_LIB)
libjcadical: SRC = $(JCADICAL_SRC)
libjcadical: CPPFLAGS += $(CADICAL_CPPFLAGS)
libjcadical: LDFLAGS += $(CADICAL_LDFLAGS)
libjcadical $(JCADICAL_LIB): $(LIB_DIR)
	@echo "=== Building libjcadical library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(LDFLAGS) $(SRC)

libjcryptominisat: LIB = $(JCRYPTOMINISAT_LIB)
libjcryptominisat: SRC = $(JCRYPTOMINISAT_SRC)
libjcryptominisat: CPPFLAGS += $(CRYPTOMINISAT_CPPFLAGS)
libjcryptominisat: LDFLAGS += $(CRYPTOMINISAT_LDFLAGS)
libjcryptominisat $(JCRYPTOMINISAT_LIB): $(LIB_DIR)
	@echo "=== Building libjcryptominisat library..."
	$(CC) -o $(LIB) $(CCFLAGS) $(CPPFLAGS) $(SRC) $(LDFLAGS)

$(LIB_DIR):
	@echo "=== Creating libdir..."
	mkdir -p $@

headers $(HEADERS): $(CLASSES_DIR)
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(CLASSPATH) $(CLASSNAMES)

classes $(CLASSES_DIR):
	@echo "=== Compiling classes..."
	./gradlew classes

res:
	@echo "=== Copying libraries to resources..."
	install -d $(LIB_RES)
	install -m 644 $(JMINISAT_LIB) $(JMINISAT_RES)
	install -m 644 $(JCADICAL_LIB) $(JCADICAL_RES)
	install -m 644 $(JCRYPTOMINISAT_LIB) $(JCRYPTOMINISAT_RES)

clean:
	@echo "=== Cleaning..."
	./gradlew clean

vars:
	$(foreach v, $(sort $(.VARIABLES)), $(if $(filter file,$(origin $(v))), $(info $(v)=$($(v)))))
