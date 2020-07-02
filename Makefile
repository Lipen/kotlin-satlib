## Dirs
BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)/classes
CLASSPATH = $(CLASSES_DIR)/kotlin/main
HEADERS_DIR = $(BUILD_DIR)/headers
LIB_DIR = $(BUILD_DIR)/lib
SRC_DIR = src/main
CPP_DIR = $(SRC_DIR)/cpp
RES_DIR = $(SRC_DIR)/resources
LIB_RES_DIR = $(RES_DIR)/lib/linux64
getSrc = $(CPP_DIR)/$(1).cpp

## MiniSat
JMINISAT_NAME = JMiniSat
JMINISAT_LIB = $(LIB_DIR)/libjminisat.so
JMINISAT_SRC = $(call getSrc,$(JMINISAT_NAME))# do not change
MINISAT_INCLUDE_DIR = /usr/local/include
MINISAT_LIB_DIR = /usr/local/lib
JMINISAT_CXXFLAGS = -fpermissive
JMINISAT_CPPFLAGS = -I$(MINISAT_INCLUDE_DIR)
JMINISAT_LDFLAGS = -L$(MINISAT_LIB_DIR)
JMINISAT_LDLIBS = -lminisat

## Glucose
JGLUCOSE_NAME = JGlucose
JGLUCOSE_LIB = $(LIB_DIR)/libjglucose.so
JGLUCOSE_SRC = $(call getSrc,$(JGLUCOSE_NAME))# do not change
GLUCOSE_INCLUDE_DIR = /usr/local/include
GLUCOSE_LIB_DIR = /usr/local/lib
JGLUCOSE_CXXFLAGS =
JGLUCOSE_CPPFLAGS = -I$(GLUCOSE_INCLUDE_DIR) -I$(GLUCOSE_INCLUDE_DIR)/glucose
JGLUCOSE_LDFLAGS = -L$(GLUCOSE_LIB_DIR)
JGLUCOSE_LDLIBS = -lglucose

## Cadical
JCADICAL_NAME = JCadical
JCADICAL_LIB = $(LIB_DIR)/libjcadical.so
JCADICAL_SRC = $(call getSrc,$(JCADICAL_NAME))# do not change
CADICAL_INCLUDE_DIR = /usr/local/include
CADICAL_LIB_DIR = /usr/local/lib
JCADICAL_CXXFLAGS =
JCADICAL_CPPFLAGS = -I$(CADICAL_INCLUDE_DIR)
JCADICAL_LDFLAGS = -L$(CADICAL_LIB_DIR)
JCADICAL_LDLIBS = -lcadical

## CryptoMiniSat
JCMS_NAME = JCryptoMiniSat
JCMS_LIB = $(LIB_DIR)/libjcms.so
JCMS_SRC = $(call getSrc,$(JCMS_NAME))# do not change
CMS_INCLUDE_DIR = /usr/local/include
CMS_LIB_DIR = /usr/local/lib
JCMS_CXXFLAGS =
JCMS_CPPFLAGS = -I$(CMS_INCLUDE_DIR)
JCMS_LDFLAGS = -L$(CMS_LIB_DIR)
JCMS_LDLIBS = -lcryptominisat5

## Another solver...
# JSOLVER_NAME = JSolver
# JSOLVER_LIB = $(LIB_DIR)/libjsolver.so
# JSOLVER_SRC = $(call getSrc,$(JSOLVER_NAME))# do not change
# SOLVER_INCLUDE_DIR = /usr/local/include
# SOLVER_LIB_DIR = /usr/local/lib
# JSOLVER_CXXFLAGS =
# JSOLVER_CPPFLAGS = -I$(SOLVER_INCLUDE_DIR)
# JSOLVER_LDFLAGS = -L$(SOLVER_LIB_DIR)
# JSOLVER_LDLIBS = -lsolver

## Common
NAMES = $(JMINISAT_NAME) $(JGLUCOSE_NAME) $(JCADICAL_NAME) $(JCMS_NAME)# ...more
LIBS = $(JMINISAT_LIB) $(JGLUCOSE_LIB) $(JCADICAL_LIB) $(JCMS_LIB)# ...more
PACKAGE = com.github.lipen.jnisat
CLASSNAMES = $(addprefix $(PACKAGE).,$(NAMES))# do not change

## Docker
DOCKER_IMAGE_NAME = kotlin-jnisat-builder
DOCKER_PROJECT_DIR = /kotlin-jnisat
DOCKER_MINISAT_DIR = /minisat
DOCKER_GLUCOSE_DIR = /glucose
DOCKER_CADICAL_DIR = /cadical
DOCKER_CMS_DIR = /cms

## Java
JAVA_HOME ?= $(patsubst %/bin/javac,%,$(realpath /usr/bin/javac))
JAVA_INCLUDE = $(JAVA_HOME)/include

## Compile/link
CXX = g++
CXXFLAGS = -Wall -O3 -fPIC -fpermissive
CPPFLAGS = -I$(JAVA_INCLUDE) -I$(JAVA_INCLUDE)/linux -I$(HEADERS_DIR)
LDFLAGS = -shared -s
LDLIBS =

.PHONY: help all all-docker libs libs-docker libjminisat libjglucose libjcadical libjcms res headers classes clean vars

define _USAGE
Specify a target! [all libs libs-docker libjminisat libjglucose libjcadical libjcms res headers classes clean vars]
  - all -- classes + headers + libs + res
  - libs -- Build all libraries
  - libs-docker -- Build all libraries using Docker
  - libjminisat/libjglucose/libjcadical/libjcms -- Build specific JNI binding library
  - res -- Copy libraries to '$(LIB_RES_DIR)'
  - headers -- Generate JNI headers from classes via javah
  - classes -- Compile Java/Kotlin classes (run 'gradlew classes')
  - clean -- Run 'gradlew clean'
  - vars -- Show Makefile variables
endef
export _USAGE
help:
	@echo "$${_USAGE}"

all: classes headers libs res
all-docker: classes headers libs-docker res
libs: $(LIBS)

libjminisat: $(JMINISAT_LIB)
$(JMINISAT_LIB): $(JMINISAT_SRC)
$(JMINISAT_LIB): CXXFLAGS += $(JMINISAT_CXXFLAGS)
$(JMINISAT_LIB): CPPFLAGS += $(JMINISAT_CPPFLAGS)
$(JMINISAT_LIB): LDFLAGS += $(JMINISAT_LDFLAGS)
$(JMINISAT_LIB): LDLIBS += $(JMINISAT_LDLIBS)

libjglucose: $(JCLUCOSE_LIB)
$(JGLUCOSE_LIB): $(JGLUCOSE_SRC)
$(JGLUCOSE_LIB): CXXFLAGS += $(JGLUCOSE_CXXFLAGS)
$(JGLUCOSE_LIB): CPPFLAGS += $(JGLUCOSE_CPPFLAGS)
$(JGLUCOSE_LIB): LDFLAGS += $(JGLUCOSE_LDFLAGS)
$(JGLUCOSE_LIB): LDLIBS += $(JGLUCOSE_LDLIBS)

libjcadical: $(JCADICAL_LIB)
$(JCADICAL_LIB): $(JCADICAL_SRC)
$(JCADICAL_LIB): CXXFLAGS += $(JCADICAL_CXXFLAGS)
$(JCADICAL_LIB): CPPFLAGS += $(JCADICAL_CPPFLAGS)
$(JCADICAL_LIB): LDFLAGS += $(JCADICAL_LDFLAGS)
$(JCADICAL_LIB): LDLIBS += $(JCADICAL_LDLIBS)

libjcms: $(JCMS_LIB)
$(JCMS_LIB): $(JCMS_SRC)
$(JCMS_LIB): CXXFLAGS += $(JCMS_CXXFLAGS)
$(JCMS_LIB): CPPFLAGS += $(JCMS_CPPFLAGS)
$(JCMS_LIB): LDFLAGS += $(JCMS_LDFLAGS)
$(JCMS_LIB): LDLIBS += $(JCMS_LDLIBS)

$(LIBS):
	@echo "=== Building $@..."
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(LDFLAGS) $^ $(LDLIBS) -o $@
	@echo "= Done building $@"

export DOCKER_IMAGE_NAME DOCKER_PROJECT_DIR LIB_DIR
libs-docker:
	@echo "=== Building libs using Docker..."
	docker build . \
		--tag $(DOCKER_IMAGE_NAME) \
		--build-arg PROJECT_DIR=$(DOCKER_PROJECT_DIR) \
		--build-arg MINISAT_DIR=$(DOCKER_MINISAT_DIR) \
		--build-arg GLUCOSE_DIR=$(DOCKER_GLUCOSE_DIR) \
		--build-arg CADICAL_DIR=$(DOCKER_CADICAL_DIR) \
		--build-arg CMS_DIR=$(DOCKER_CMS_DIR)
	./scripts/build-libs-docker.sh
	@echo "= Done building libs using Docker"

res:
	@echo "=== Copying libraries to resources: '$(LIB_RES_DIR)'..."
	install -m 644 $(LIBS) -Dt $(LIB_RES_DIR)
	@echo "= Done copying libraries to resources"

headers:
	@echo "=== Generating headers..."
	javah -d $(HEADERS_DIR) -classpath $(CLASSPATH) $(CLASSNAMES)
	@echo "= Done generating headers"

classes:
	@echo "=== Compiling classes..."
	./gradlew classes
	@echo "= Done compiling classes"

clean:
	@echo "=== Cleaning..."
	./gradlew clean
	@echo "= Done cleaning"

clean-res:
	@echo "=== Cleaning resources..."
	rm -f $(addprefix $(LIB_RES_DIR)/,$(notdir $(LIBS)))
	@echo "= Done cleaning resources"

vars:
	@: $(foreach v,$(sort $(.VARIABLES)),$(if $(filter file,$(origin $(v))),$(info $(v)=$($(v)))))
