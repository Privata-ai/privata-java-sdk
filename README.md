# Java SDK for Blockbird.data API

This SDK can be used in Java Applications to send audit data to the blockbird.data API

# Registering your Application

In order to register your application, you must create an account on [blockbird.ventures](https://blockbird.ventures/data/#). When your account has been created, you can register your Application and select the sensitive data within your Database.

During this process, you will receive a **Database Key** and **Database Secret** which is used to authenticate and identify your Application and Database to our API.

You can store your `DbKey` and `DbSecret` as environmental variables in your java project. These are private credentials and must not be shared or made public.

## Installation

Import to Maven using the following in your `pom.xml`:

```xml
    <dependencies>
      <!-- Blockbird data additions -->
      <dependency>
         <groupId>ventures.blockbird.data</groupId>
         <artifactId>data-blockbird-sdk</artifactId>
         <version>0.1-SNAPSHOT</version>
      </dependency>
      ...
    </dependencies>
```

## Usage

To record a auditable transaction, you first instantiate the `BlockbirdAudit` Object using the URL of the API, the `dbKey` and `dbSecret` that you received during the On-Boarding process.

You can instantiate the object like this:

```java
// add Blockbird Audit
bbAudit = BlockbirdAudit.getInstance("URL-of-API","dbKey","dbSecret");
```

Then you add queries to your packet:

```java
bbAudit.addQuery(clientUserId, clientRole, clientTable, clientRole, action("Create" | "Read" | "Update" | "Delete"), actionDate, rowsAffected);

```

Queries are batched in order to reduce network traffic. If you wish to force sending the current batch of queries, you can run:

```java
bbAudit.run();
```
