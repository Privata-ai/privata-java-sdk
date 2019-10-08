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

To use blockbird.data, you must update you `openmrs-runtime.properties` file and add the following parameters:
```
blockbird.username=<Your blockbird.data username>
blockbird.password=<Your blockbird.data password>
blockbird.url=<The URL of blockbird.data's API>
blockbird.appId=<The ID of your Application - from the blockbird.data dashboard>
blockbird.dbId=<The ID of your Database - from the blockbird.data dashboard>

```

To record a auditable transaction, you first initiate Blockbird:

``` java
		// add Blockbird Audit
		Properties props  = Context.getRuntimeProperties();
		bbAudit = new BlockbirdAudit(
			props.getProperty("blockbird.url"), 
			props.getProperty("blockbird.appId"), 
			props.getProperty("blockbird.dbId"), 
			props.getProperty("blockbird.username"), 
			props.getProperty("blockbird.password"));
```

Then you add queries to your packet:
``` java
bbAudit.addQuery(clientUserId, clientRole, clientTable, clientRole, action("Create" | "Read" | "Update" | "Delete"), actionDate, rowsAffected);

```

When you want to send to the API, you run:
``` java
bbAudit.run();
```