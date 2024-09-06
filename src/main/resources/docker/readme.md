Стартуем - ```docker-compose up -d```

## Настройка мастера

1. ```
   docker exec -it mysql-master bash
   mysql -uroot -p
   ```
2. ```
   ALTER USER 'replication_user'@'%' IDENTIFIED WITH 'mysql_native_password' BY '123123123';
   GRANT REPLICATION SLAVE ON *.* TO 'replication_user'@'%';
   FLUSH PRIVILEGES;
   SHOW MASTER STATUS; - здесь нам нужно будет сохранить значения колонок File и Position
   ```

## Настройка реплики

1. ```
   docker exec -it mysql-slave bash
   mysql -uroot -p
   ```
2. ```
   CHANGE MASTER TO
     MASTER_HOST='mysql-master',
     MASTER_USER='replication_user',
     MASTER_PASSWORD='123123123',
     MASTER_LOG_FILE='значение колонки File из настроек мастера',
     MASTER_LOG_POS=значение колонки Position из настроек мастера;
   ```
3. ```START SLAVE;```
4. ```SHOW SLAVE STATUS\G - убедиться, что Slave_IO_Running и Slave_SQL_Running в значениях Yes```