# Java SDK for Privata.ai API

This SDK can be used in Java Applications to send audit data to the privata.ai API

# Registering your Application

In order to register your application, you must create an account on [privata.ai](https://privata.ai). When your account has been created, you can register your Application and select the sensitive data within your Database.

During this process, you will receive a **Database Key** and **Database Secret** which is used to authenticate and identify your Application and Database to our API.

You can store your `DbKey` and `DbSecret` as environmental variables in your java project. These are private credentials and must not be shared or made public.

## Installation

Import to Maven using the following in your `pom.xml`:

```xml
    <dependencies>
      <!-- Privata.ai additions -->
      <dependency>
        <groupId>ai.privata</>
        <artifactId>privata-ai-sdk</artifactId>
        <version>0.3-SNAPSHOT</version>
      </dependency>
      ...
    </dependencies>
```

## Test

For test purpose use the `firebaseApiKeyLocal` when instantiating `FirebaseAuth`.

## Usage

To record a auditable transaction, you first instantiate the `PrivataAudit` Object using the URL of the API, the `dbKey` and `dbSecret` that you received during the On-Boarding process.

You can instantiate the object like this:

```java
// add Privata Audit
privataAudit = new PrivataAudit(boolean sandbox, String apiUrl);
OR
privataAudit = new PrivataAudit(boolean sandbox);
OR
privataAudit = new PrivataAudit(String apiUrl);
OR
privataAudit = new PrivataAudit();
```

Then initialize the app:

```java
privataAudit.initialize(String dbKey, String dbSecret);
```

Then you send queries to api:

```java
privataAudit.sendQueries(JsonArray queries);

```

When sending queries, the Tables and Columns that have been flagged as containing Personal Data during the On-boarding phase will be retrieved.

> Note: Privata.ai saves table and column names in `camelCase` format.
