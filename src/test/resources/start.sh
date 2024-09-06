#!/bin/bash

# Подойдут любые два инстанса mysql. Настройка в sharding.yml

docker run --name mysql-shard-1 -p 13306:3306 -e MYSQL_ROOT_PASSWORD=pwd12345 -e MYSQL_DATABASE=shard_test -d mysql;
docker run --name mysql-shard-2 -p 13307:3306 -e MYSQL_ROOT_PASSWORD=pwd12345 -e MYSQL_DATABASE=shard_test -d mysql;