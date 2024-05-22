#!/bin/bash

nmvn clean package -DskipTests
mv -f ./producer/target/producer-0.0.1-SNAPSHOT.jar ./kafka/producer/producer-0.0.1-SNAPSHOT.jar
cd ./kafka/producer
docker build . --tag kafka-producer-img || exit 0
docker run -itd --rm --name kafka-producer --network kafka_bridge kafka-producer-img