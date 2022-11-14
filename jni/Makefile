## Dirs
BUILD_DIR = build
LIB_DIR = $(BUILD_DIR)/lib
SRC_DIR = src/main
CPP_DIR = $(SRC_DIR)/cpp
RES_DIR = $(SRC_DIR)/resources
RES_LIB_SUBDIR = linux64# `linux64` or `osx64` or `win64`
RES_LIB_DIR = $(RES_DIR)/lib/$(RES_LIB_SUBDIR)
LIB_PREFIX = lib# `lib` or empty
LIB_EXT = so# `so` or `dll` or `dylib`
getSrc = $(CPP_DIR)/$(1).cpp
getLib = $(LIB_DIR)/$(LIB_PREFIX)$(1).$(LIB_EXT)

## MiniSat
JMINISAT_NAME = JMiniSat
JMINISAT_LIB_NAME = jminisat
JMINISAT_LIB = $(call getLib,$(JMINISAT_LIB_NAME))#do not change
JMINISAT_SRC = $(call getSrc,$(JMINISAT_NAME))# do not change
MINISAT_INSTALL_DIR = /usr/local
MINISAT_INCLUDE_DIR = $(MINISAT_INSTALL_DIR)/include
MINISAT_LIB_DIR = $(MINISAT_INSTALL_DIR)/lib
JMINISAT_CXXFLAGS = -fpermissive
JMINISAT_CPPFLAGS = -I$(MINISAT_INCLUDE_DIR)
JMINISAT_LDFLAGS = -L$(MINISAT_LIB_DIR)
JMINISAT_LDLIBS = -lminisat

## Glucose
JGLUCOSE_NAME = JGlucose
JGLUCOSE_LIB_NAME = jglucose
JGLUCOSE_LIB = $(call getLib,$(JGLUCOSE_LIB_NAME))#do not change
JGLUCOSE_SRC = $(call getSrc,$(JGLUCOSE_NAME))# do not change
GLUCOSE_INSTALL_DIR = /usr/local
GLUCOSE_INCLUDE_DIR = $(GLUCOSE_INSTALL_DIR)/include
GLUCOSE_LIB_DIR = $(GLUCOSE_INSTALL_DIR)/lib
JGLUCOSE_CXXFLAGS =
JGLUCOSE_CPPFLAGS = -I$(GLUCOSE_INCLUDE_DIR) -I$(GLUCOSE_INCLUDE_DIR)/glucose
JGLUCOSE_LDFLAGS = -L$(GLUCOSE_LIB_DIR)
JGLUCOSE_LDLIBS = -lglucose

## Cadical
JCADICAL_NAME = JCadical
JCADICAL_LIB_NAME = jcadical
JCADICAL_LIB = $(call getLib,$(JCADICAL_LIB_NAME))#do not change
JCADICAL_SRC = $(call getSrc,$(JCADICAL_NAME))# do not change
CADICAL_INSTALL_DIR = /usr/local
CADICAL_INCLUDE_DIR = $(CADICAL_INSTALL_DIR)/include
CADICAL_LIB_DIR = $(CADICAL_INSTALL_DIR)/lib
JCADICAL_CXXFLAGS =
JCADICAL_CPPFLAGS = -I$(CADICAL_INCLUDE_DIR)
JCADICAL_LDFLAGS = -L$(CADICAL_LIB_DIR)
JCADICAL_LDLIBS = -lcadical

## CryptoMiniSat
JCMS_NAME = JCryptoMiniSat
JCMS_LIB_NAME = jcms
JCMS_LIB = $(call getLib,$(JCMS_LIB_NAME))#do not change
JCMS_SRC = $(call getSrc,$(JCMS_NAME))# do not change
CMS_INSTALL_DIR = /usr/local
CMS_INCLUDE_DIR = $(CMS_INSTALL_DIR)/include
CMS_LIB_DIR = $(CMS_INSTALL_DIR)/lib
JCMS_CXXFLAGS =
JCMS_CPPFLAGS = -I$(CMS_INCLUDE_DIR)
JCMS_LDFLAGS = -L$(CMS_LIB_DIR)
JCMS_LDLIBS = -lcryptominisat5

## Another solver...
# JSOLVER_NAME = JSolver
# JSOLVER_LIB_NAME = jsolver
# JSOLVER_LIB = $(call getLib,$(JSOLVER_LIB_NAME))#do not change
# JSOLVER_SRC = $(call getSrc,$(JSOLVER_NAME))# do not change
# SOLVER_INCLUDE_DIR = /usr/local/include
# SOLVER_LIB_DIR = /usr/local/lib
# JSOLVER_CXXFLAGS =
# JSOLVER_CPPFLAGS = -I$(SOLVER_INCLUDE_DIR)
# JSOLVER_LDFLAGS = -L$(SOLVER_LIB_DIR)
# JSOLVER_LDLIBS = -lsolver

## Common
LIBS = $(JMINISAT_LIB) $(JGLUCOSE_LIB) $(JCADICAL_LIB) $(JCMS_LIB)# ...more

## Java
JAVA_HOME ?= $(patsubst %/bin/javac,%,$(realpath /usr/bin/javac))
JAVA_INCLUDE = $(JAVA_HOME)/include
JAVA_INCLUDE_SUBDIR = linux# `linux` or `darwin` or `win32`

## Compile/link
CXX = g++
CXXFLAGS += -Wall -O3 -fPIC
CPPFLAGS += -I"$(JAVA_INCLUDE)" -I"$(JAVA_INCLUDE)/$(JAVA_INCLUDE_SUBDIR)"
LDFLAGS += -shared
LDLIBS =

.PHONY: help all libs jminisat jglucose jcadical jcms res clean vars

define _USAGE
Specify a target! [all libs jminisat jglucose jcadical jcms res clean vars]
  - all -- libs + res
  - libs -- Build all libraries
  - jminisat/jglucose/jcadical/jcms -- Build specific JNI binding library
  - res -- Copy libraries to '$(RES_LIB_DIR)'
  - clean -- Run 'gradlew clean'
  - vars -- Show Makefile variables
endef
export _USAGE
help:
	@echo "$${_USAGE}"

all: libs res
libs: $(LIBS)

jminisat: $(JMINISAT_LIB)
$(JMINISAT_LIB): $(JMINISAT_SRC)
$(JMINISAT_LIB): CXXFLAGS += $(JMINISAT_CXXFLAGS)
$(JMINISAT_LIB): CPPFLAGS += $(JMINISAT_CPPFLAGS)
$(JMINISAT_LIB): LDFLAGS += $(JMINISAT_LDFLAGS)
$(JMINISAT_LIB): LDLIBS += $(JMINISAT_LDLIBS)

jglucose: $(JGLUCOSE_LIB)
$(JGLUCOSE_LIB): $(JGLUCOSE_SRC)
$(JGLUCOSE_LIB): CXXFLAGS += $(JGLUCOSE_CXXFLAGS)
$(JGLUCOSE_LIB): CPPFLAGS += $(JGLUCOSE_CPPFLAGS)
$(JGLUCOSE_LIB): LDFLAGS += $(JGLUCOSE_LDFLAGS)
$(JGLUCOSE_LIB): LDLIBS += $(JGLUCOSE_LDLIBS)

jcadical: $(JCADICAL_LIB)
$(JCADICAL_LIB): $(JCADICAL_SRC)
$(JCADICAL_LIB): CXXFLAGS += $(JCADICAL_CXXFLAGS)
$(JCADICAL_LIB): CPPFLAGS += $(JCADICAL_CPPFLAGS)
$(JCADICAL_LIB): LDFLAGS += $(JCADICAL_LDFLAGS)
$(JCADICAL_LIB): LDLIBS += $(JCADICAL_LDLIBS)

jcms: $(JCMS_LIB)
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

res:
	@echo "=== Copying libraries to resources: '$(RES_LIB_DIR)'..."
	install -m 644 $(LIBS) -Dt $(RES_LIB_DIR)
	@echo "= Done copying libraries to resources"

clean:
	@echo "=== Cleaning resources..."
	rm -f $(addprefix $(RES_LIB_DIR)/,$(notdir $(LIBS)))
	@echo "= Done cleaning resources"

vars:
	@: $(foreach v,$(sort $(.VARIABLES)),$(if $(filter file,$(origin $(v))),$(info $(v)=$($(v)))))
