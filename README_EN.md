# Tool Introduction

This tool migrates a PostgreSQL 13 database to an openGauss database.  

If the PostgreSQL 13 database is running on host A and the openGauss database is running on host B, you need to migrate the DBNAME of the PostgreSQL 13 database to the openGauss database.

# Scope

- During the database migration, the services provided by the database to the client are limited (only DML is supported).  

  Debezium in the online migration module can capture only row-level changes to the PostgreSQL database, such as insert, update, and delete operations, but not the changes to the PostgreSQL database table structure and table indexes. During the database migration, the services provided by the database to the client are limited. The client can only perform row-level changes such as insert, update, and delete on existing physical tables. It cannot create new tables, modify table structures, or alter indexes.

- Only data in the PostgreSQL database can be automatically migrated. Objects such as table structures and indexes in the PostgreSQL database cannot be automatically migrated.

  You need to manually create objects such as the table structure and indexes of the PostgreSQL database in the openGauss database.  

- Data types

  Only common data types can be migrated: numeric types (integer and decimal), string types, and one-dimensional arrays.

  Data types interval, JSON, XML, bit, and point are not supported.

  The date and time types must consider time zones, which may impact the migration accuracy.

- Special tables

  Partitioned tables can be migrated, but they must have primary keys.

  Unlogged tables cannot be migrated.

- Resumable transfer is not supported.

# Migration Preparation

- Check whether ports are occupied.

  | ZooKeeper| Kafka| Debezium|
  | --------- | ----- | -------- |
  | 2181      | 9092  | 8083     |

  If the preceding ports are occupied, you are advised to kill the processes that occupy the ports to release the ports.

- PostgreSQL 13

  - Installing wal2json

    ```shell
    git clone https://github.com/eulerto/wal2json.git
    cd wal2json
    make
    make install
    ```
    
  - Modify the database configuration file `postgresql.conf`.

    1. Adding content

       ```shell
       shared_preload_libraries = 'wal2json'
       ```

    2. Specifying content

       ```shell
       wal_level = logical             
       max_wal_senders = 1             
       max_replication_slots = 1       
       ```
    
  - Setting the listener for PostgreSQL 13

    You need to configure that host B can connect to the PostgreSQL 13 database on host A.  

    1. Modifying the `postgresql.conf` file  

       ```shell
       #listen_address='localhost' now changed to
       
       listen_address='*'
       ```

    2. Modifying the `pg_hba.conf` file

       Added:

       ```shell
       host	all		all		192.168.0.1/32		trust
       ```

       192.168.0.1/32 is the openGauss IP address.

  - Enabling replication using the Debezium connector host for PostgreSQL 13

    Configuring the `pg_hba.conf` file

    ```shell
    local   replication     all                          trust   
    host    replication     all  127.0.0.1/32            trust   
    host    replication     all  ::1/128                 trust   
    ```

  - Restarting the PostgreSQL 13 database.

    ```shell
    pg_ctl stop -D XXX/data
    pg_ctl start -D XXX/data
    ```
    
  - Check whether the tables (non-partitioned tables) in the database to be migrated have primary keys.

    wal2json cannot parse the insert and delete operations of tables without a primary key. For such tables, perform the following operations:

    ```sql
    alter table tablename replica identity full;
    ```

  - Creating a replication user

    For security purposes, you are advised to create a replication user with the least privileges.

    ```sql
    CREATE ROLE <copyuser> with password 'password' REPLICATION LOGIN;
    CREATE ROLE <replication_group>;
    GRANT REPLICATION_GROUP TO <original_owner>;
    GRANT REPLICATION_GROUP TO <copyuser>;
    ALTER TABLE <table_name> OWNER TO <replication_group>;
    ```
  
- openGauss

  - Creating a replication user on openGauss for JDBC connection

    ```sql
    create user <testuser> with password 'password';
    grant all privileges to <testuser>;
    ```

  - Configuring client access authentication for openGauss
  
    Adding the following settings to `pg_hba.conf`:
  
    ```shell
    host all all 127.0.0.1/32 sha256
    ```
  
  - Adding a listener to openGauss
  
    Adding the following settings to `postgresql.conf`:
  
    ```
    #listen_addresses='localhost' now changed to
    
    listen_addresses='*'
    ```
  
  - Creating a receiving database
  
    This tool only migrates data in the database. You need to manually create database table structures and indexes in openGauss.
    
    Note that when creating an openGauss database, you need to set `DBCOMPATIBILITY` to 'PG' to be compatible with the PostgreSQL database. Example: CREATE DATABASE <og_dbname> DBCOMPATIBILITY 'PG'
    
  - Restarting the openGauss database
  
    ```shell
    gs_ctl restart -D XXX/data
    ```

# Migration Tool Usage

Copy the migration tool to the host where openGauss resides.

- mvn

  Recommended: 3.6.3

- Java Version

  Java 11

- Configuring the PostgreSQL connector

  Configuration file path: openGauss2postgresql13-tools-migration\config\register-pg13-xtreams.json

  ```json
  {
    "name": "fulfillment-connector",  
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector", 
      "database.hostname": "0.0.0.0", 
      "database.port": "5432", 
      "database.user": "<copyuser>", 
      "database.password": "password", 
      "database.dbname" : "pg_dbname", 
      "database.server.name": "fulfillment", 
      "plugin.name":"wal2json",
      "slot.name":"slotname",
      "provide.transaction.metadata":"true",
      "snapshot.mode":"custom",
      "snapshot.custom.class":"io.debezium.connector.postgresql.snapshot.PgDumpSnapshotter"
    }
  }
  ```

  Settings to be modified:

  | Name         | Value After Change                         |
  | ----------------- | ------------------------------------- |
  | database.hostname | IP address of host A where the PostgreSQL 13 database is installed|
  | database.port     | Connection port of the PostgreSQL 13 database         |
  | database.user     | Name of the replication user created in the PostgreSQL 13 database |
  | database.password | Replication user password                       |
  | database.dbname   | Name of the database to be migrated                       |

- Configuring JDBC on the consumer side

  Configuration file path: openGauss2postgresql13-tools-migration\config\consumer_setting.properties

  ```shell
  bootstrap.servers=localhost:9092
  enable.auto.commit=False
  auto.commit.interval.ms=1000
  key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
  value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
  lsnfile=lsn.txt
  database.server.name=fulfillment
  group.id=consumer
  database.driver.classname=org.postgresql.Driver
  database.url=jdbc:postgresql://127.0.0.1:5432/og_dbname?stringtype=unspecified
  database.user=<testuser>
  database.password=password
  ```

  Settings to be modified:

  | Name            | Value After Change                                                |
  | -------------------- | ------------------------------------------------------------ |
  | database.server.name | Same as that of `database.server.name` in `register-pg13-xtreams.json`  |
  | database.url         | jdbc:postgresql://127.0.0.1:port/dbname?stringtype=unspecified, where `port` indicates the port number for connecting to openGauss, `dbname` indicates the name of the database to be migrated, and the time zone must be the same as that of the PostgreSQL database.|
  | database.user        | Name of the replication user created in the openGauss database                               |
  | database.password    | Replication user password                                              |

- Configuring openGauss2postgresql13-tools-migration\migration.sh

  | Variable   | Content                              |
  | ----------- | -------------------------------------- |
  | SCHEMA      | Schema to be migrated. If no schema exists, enter `public`.|
  | PG_HOST     | IP address of the host where PostgreSQL runs                    |
  | PG_USER     | Installation user of PostgreSQL                    |
  | PG_PORT     | Port used to connect to the PostgreSQL database to be migrated          |
  | PG_DBNAME   | Name of the PostgreSQL database to be migrated              |
  | PG_DATA_DIR | Directory for storing intermediate data on the Postgres database          |
  | OG_PORT     | Connection port of the local openGauss database           |
  | OG_DBNAME   | Name of the local openGauss database         |
  | OG_DATA_DIR | Directory for storing intermediate data on the openGauss database         |

- Running the shell script

  `sh migration.sh`
