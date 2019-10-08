# Java SDK for Blockbird.data API
This SDK can be used in Java Applications to send audit data to the blockbird.data API

## Installation
Import to Maven using the following in your `pom.xml`:
``` xml
    <dependencies>
      <!-- Blockbird data additions -->
      <dependency>
         <groupId>ventures.blockbird.data</groupId>
         <artifactId>data-blockbird-sdk</artifactId>
         <version>0.1-SNAPSHOT</version>
      </dependency>
      <dependency>
         <groupId>com.googlecode.json-simple</groupId>
         <artifactId>json-simple</artifactId>
         <version>1.1.1</version>
      </dependency>
      ...
    </dependencies>
```

## Usage

To record a auditable transaction, you first initiate Blockbird:

``` java
// add Blockbird Audit
bbAudit = new BlockbirdAudit("URL-of-API","appId", "dbId", "username", "password");
```

Then you add queries to your packet:
``` java
bbAudit.addQuery(clientUserId, clientRole, clientTable, clientRole, action("Create" | "Read" | "Update" | "Delete"), actionDate, rowsAffected);

```

When you want to send to the API, you run:
``` java
bbAudit.run();
```
