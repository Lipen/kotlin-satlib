## Dirs
BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)/classes
HEADERS_DIR = $(BUILD_DIR)/headers
SRC_DIR = src/main
CPP_DIR = $(SRC_DIR)/cpp
LIB_DIR = $(BUILD_DIR)/lib
RES_DIR = $(SRC_DIR)/resources
LIB_RES_DIR = $(RES_DIR)/lib/linux64

## MiniSat
JMINISAT_CLASSNAME = JMiniSat
JMINISAT_SRC = $(CPP_DIR)/JMiniSat.cpp
JMINISAT_LIB = $(LIB_DIR)/libjminisat.so
MINISAT_INCLUDE_DIR = /usr/local/include
MINISAT_LIB_DIR = /usr/local/lib
MINISAT_LDLIB = minisat

## Cadical
JCADICAL_CLASSNAME = JCadical
JCADICAL_SRC = $(CPP_DIR)/JCadical.cpp
JCADICAL_LIB = $(LIB_DIR)/libjcadical.so
CADICAL_INCLUDE_DIR = /usr/local/include
CADICAL_LIB_DIR = /usr/local/lib
CADICAL_LDLIB = cadical

## CryptoMiniSat
JCMS_CLASSNAME = JCryptoMiniSat
JCMS_SRC = $(CPP_DIR)/JCryptoMiniSat.cpp
JCMS_LIB = $(LIB_DIR)/libjcms.so
CMS_INCLUDE_DIR = /usr/local/include
CMS_LIB_DIR = /usr/local/lib
CMS_LDLIB = cryptominisat5

## Java
JAVA_HOME ?= $(patsubst %/bin/javac,%,$(realpath /usr/bin/javac))
JAVA_INCLUDE = $(JAVA_HOME)/include
CLASSPATH = $(CLASSES_DIR)/kotlin/main
PACKAGE = com.github.lipen.jnisat
CLASSNAMES = $(JMINISAT_CLASSNAME) $(JCADICAL_CLASSNAME) $(JCMS_CLASSNAME) # ...more
CLASSNAMES_FULL = $(addprefix $(PACKAGE).,$(CLASSNAMES))
HEADERS = $(foreach s,$(CLASSNAMES_FULL),$(HEADERS_DIR)/$(subst .,_,$(s)).h)
CLASSES = $(foreach s,$(CLASSNAMES_FULL),$(CLASSPATH)/$(subst .,/,$(s)).class)
LIBS = $(JMINISAT_LIB) $(JCADICAL_LIB) $(JCMS_LIB) # ...more

## Docker
DOCKER_IMAGE_NAME = kotlin-jnisat-builder
DOCKER_PROJECT_DIR = /kotlin-jnisat
DOCKER_MINISAT_DIR = /minisat
DOCKER_CADICAL_DIR = /cadical
DOCKER_CMS_DIR = /cms
DOCKER_MINISAT_IMAGE_NAME = minisat-builder
DOCKER_CADICAL_IMAGE_NAME = cadical-builder
DOCKER_CMS_IMAGE_NAME = cms-builder

## Compile/link
CXX = g++
CXXFLAGS = -Wall -O3 -fPIC -fpermissive
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS = -shared -s
LDLIBS =

.PHONY: help all libs libs-docker libjminisat libjcadical libjcms res headers classes clean vars

define _USAGE
Specify a target! [all libs libs-docker libjminisat libjcadical libjcms res headers classes clean vars]
  - all -- headers + libs + res
  - libs -- Build all libraries
  - libs-docker -- Build all libraries using Docker
  - libjminisat/libjcadical/libjcms -- Build specific JNI binding library
  - res -- Copy libraries to '$(LIB_RES_DIR)'
  - headers -- Generate JNI headers from classes via javah
  - classes -- Compile Java/Kotlin classes (run 'gradlew classes')
  - clean -- Run 'gradlew clean'
  - vars -- Show Makefile variables
endef
export _USAGE
help:
	@echo "$${_USAGE}"

all: headers libs res
libs: $(LIBS)
libjminisat: $(JMINISAT_LIB)
libjcadical: $(JCADICAL_LIB)
libjcms: $(JCMS_LIB)

$(JMINISAT_LIB): $(JMINISAT_SRC)
$(JMINISAT_LIB): CPPFLAGS += -I$(MINISAT_INCLUDE_DIR)
$(JMINISAT_LIB): LDFLAGS += -L$(MINISAT_LIB_DIR)
$(JMINISAT_LIB): LDLIBS += -l$(MINISAT_LDLIB)

$(JCADICAL_LIB): $(JCADICAL_SRC)
$(JCADICAL_LIB): CPPFLAGS += -I$(CADICAL_INCLUDE_DIR)
$(JCADICAL_LIB): LDFLAGS += -L$(CADICAL_LIB_DIR)
$(JCADICAL_LIB): LDLIBS += -l$(CADICAL_LDLIB)

$(JCMS_LIB): $(JCMS_SRC)
$(JCMS_LIB): CPPFLAGS += -I$(CMS_INCLUDE_DIR)
$(JCMS_LIB): LDFLAGS += -L$(CMS_LIB_DIR)
$(JCMS_LIB): LDLIBS += -l$(CMS_LDLIB)

$(LIBS): $(HEADERS)
	@echo "=== Building $@..."
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(LDFLAGS) $^ $(LDLIBS) -o $@
	@echo "=== Done building $@"

export DOCKER_IMAGE_NAME DOCKER_PROJECT_DIR LIB_DIR
libs-docker:
	@echo "=== Building libs using Docker..."
	docker build . \
		--tag $(DOCKER_IMAGE_NAME) \
		--build-arg PROJECT_DIR=$(DOCKER_PROJECT_DIR) \
		--build-arg MINISAT_DIR=$(DOCKER_MINISAT_DIR) \
		--build-arg CADICAL_DIR=$(DOCKER_CADICAL_DIR) \
		--build-arg CMS_DIR=$(DOCKER_CMS_DIR)
	./scripts/build-libs-docker.sh
	@echo "=== Done building libs using Docker"

res: $(LIBS)
	@echo "=== Copying libraries to resources..."
	install -m 644 $(JMINISAT_LIB) -Dt $(LIB_RES_DIR)
	install -m 644 $(JCADICAL_LIB) -Dt $(LIB_RES_DIR)
	install -m 644 $(JCMS_LIB) -Dt $(LIB_RES_DIR)
	@echo "=== Done copying libraries to resources"

headers $(HEADERS): $(CLASSES)
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(CLASSPATH) $(CLASSNAMES_FULL)
	@echo "=== Done generating headers"

classes $(CLASSES):
	@echo "=== Compiling classes..."
	./gradlew classes
	@echo "=== Done compiling classes"

clean:
	@echo "=== Cleaning..."
	./gradlew clean
	@echo "=== Done cleaning"

clean-res:
	@echo "=== Cleaning resources..."
	rm -f $(LIB_RES_DIR)/$(notdir $(JMINISAT_LIB))
	rm -f $(LIB_RES_DIR)/$(notdir $(JCADICAL_LIB))
	rm -f $(LIB_RES_DIR)/$(notdir $(JCMS_LIB))
	@echo "=== Done cleaning resources"

vars:
	@: $(foreach v,$(sort $(.VARIABLES)),$(if $(filter file,$(origin $(v))),$(info $(v)=$($(v)))))
