ARG PROJECT_DIR=/kotlin-jnisat
ARG MINISAT_DIR=/minisat
ARG CADICAL_DIR=/cadical
ARG CMS_DIR=/cms

FROM openjdk:8 as builder
ARG PROJECT_DIR
ARG MINISAT_DIR
ARG CADICAL_DIR
ARG CMS_DIR

ENV PROJECT_DIR=${PROJECT_DIR}
ENV MINISAT_DIR=${MINISAT_DIR}
ENV CADICAL_DIR=${CADICAL_DIR}
ENV CMS_DIR=${CMS_DIR}

RUN apt-get update &&\
    apt-get install --no-install-recommends -y \
        build-essential \
        cmake \
        gcc \
        git \
        help2man \
        libboost-program-options-dev \
        libm4ri-dev \
        libsqlite3-dev \
        zlib1g-dev \
    &&\
    rm -rf /var/lib/apt/lists/*

## Build MiniSat
WORKDIR ${MINISAT_DIR}
RUN git clone https://github.com/niklasso/minisat .
RUN make lsh MINISAT_REL='-O3 -DNDEBUG -fpermissive'
RUN make install

## Build Cadical
WORKDIR ${CADICAL_DIR}
RUN git clone https://github.com/arminbiere/cadical .
COPY cadical-shared-lib.patch .
RUN git apply cadical-shared-lib.patch
RUN ./configure -j8 -fPIC CXXFLAGS="-s"
RUN make lsh
RUN install -d /usr/local/include/cadical
RUN install -m 644 src/cadical.hpp /usr/local/include/cadical
RUN install -d /usr/local/lib
RUN install -m 644 build/libcadical.so /usr/local/lib

## Build CryptoMiniSat
WORKDIR ${CMS_DIR}
RUN git clone https://github.com/msoos/cryptominisat .
RUN mkdir build
WORKDIR build
RUN cmake ..
RUN make -j8
RUN make install

## Build libs
WORKDIR ${PROJECT_DIR}
COPY . .
# Note: 'make headers' must be executed outside
RUN make libs
