ARG DIR=/kotlin-jnisat

FROM openjdk:8 as builder
ARG DIR

RUN apt-get update &&\
    apt-get install --no-install-recommends -y \
        build-essential \
        gcc \
        git \
        wget \
        zlib1g-dev \
    &&\
    rm -rf /var/lib/apt/lists/*

## Build libminisat
WORKDIR /minisat
RUN git clone https://github.com/niklasso/minisat .
RUN make lsh MINISAT_REL='-O3 -DNDEBUG -fpermissive'
RUN make install

## Build libcadical
WORKDIR /cadical
RUN git clone https://github.com/arminbiere/cadical .
COPY cadical-shared-lib.patch .
RUN git apply cadical-shared-lib.patch
RUN ./configure -j8 -fPIC CXXFLAGS="-s"
RUN make -C build libcadical.so
RUN install -d /usr/local/include/cadical
RUN install -m 644 src/cadical.hpp /usr/local/include/cadical
RUN install -d /usr/local/lib
RUN install -m 644 build/libcadical.so /usr/local/lib

## Build libs
WORKDIR $DIR
COPY . .
# Note: 'make headers' must be executed outside
RUN make libs
