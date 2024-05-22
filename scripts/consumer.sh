#!/bin/bash

nmvn clean package -DskipTests
mv -f ./consumer/target/consumer-0.0.1-SNAPSHOT.jar ./kafka/consumer/consumer-0.0.1-SNAPSHOT.jar
cd ./kafka/consumer
docker build . --tag kafka-consumer-img || exit 0
docker run -itd --rm --name kafka-consumer --network kafka_bridge kafka-consumer-img