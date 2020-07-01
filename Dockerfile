ARG PROJECT_DIR=/kotlin-jnisat
ARG MINISAT_DIR=/minisat
ARG GLUCOSE_DIR=/glucose
ARG CADICAL_DIR=/cadical
ARG CMS_DIR=/cms

FROM openjdk:8 as builder

ARG PROJECT_DIR
ARG MINISAT_DIR
ARG CADICAL_DIR
ARG GLUCOSE_DIR
ARG CMS_DIR

RUN apt-get update &&\
    apt-get install --no-install-recommends --no-install-suggests -y \
        build-essential \
        ca-certificates \
        cmake \
        git \
        libboost-program-options-dev \
        zlib1g-dev \
    &&\
    rm -rf /var/lib/apt/lists/*

## Build MiniSat
WORKDIR ${MINISAT_DIR}
RUN git clone --depth=1 https://github.com/niklasso/minisat .
RUN make lsh MINISAT_REL='-O3 -DNDEBUG -fpermissive'
RUN make install
RUN strip --strip-unneeded /usr/local/lib/libminisat.so

## Build Glucose
WORKDIR ${GLUCOSE_DIR}
RUN git clone --depth=1 https://github.com/wadoon/glucose .
COPY patches/glucose-install.patch .
RUN git apply glucose-install.patch
RUN mkdir build
WORKDIR build
RUN cmake .. -DBUILD_SHARED_LIBS=ON
RUN make -j8
RUN make install/strip

## Build Cadical
WORKDIR ${CADICAL_DIR}
RUN git clone --depth=1 --branch rel-1.3.0 https://github.com/arminbiere/cadical .
COPY patches/cadical-shared-lib.patch .
RUN git apply cadical-shared-lib.patch
RUN ./configure -j8 -fPIC CXXFLAGS="-s"
RUN make lsh
RUN install -m 644 src/cadical.hpp -Dt /usr/local/include/cadical
RUN install -m 644 build/libcadical.so -Dt /usr/local/lib
RUN strip --strip-unneeded /usr/local/lib/libcadical.so
# RUN make cadical
# RUN install -m 755 build/cadical -Dt /usr/local/bin

## Build CryptoMiniSat
WORKDIR ${CMS_DIR}
RUN git clone --depth=1 --branch 5.7.1 https://github.com/msoos/cryptominisat .
RUN mkdir build
WORKDIR build
RUN cmake -DENABLE_PYTHON_INTERFACE=OFF ..
RUN make -j8
RUN make install/strip

## Build JNI libs
WORKDIR ${PROJECT_DIR}
COPY . .
# Note: 'make headers' must be executed outside!
RUN make libs LIB_DIR=build/lib
