package ai.privata;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ai.privata.util.PropsUtil;

/**
 * Unit test for simple App.
 */
public class PrivataAuditTest {
    private static PrivataAudit privataAudit;

    /**
     * Create new privataAudit instance and initialize
     *
     * @param testName name of the test case
     */
    @BeforeClass
    public static void initPrivataAudit() {
        final PropsUtil util = new PropsUtil("/project.properties");
        boolean exceptionThrown = false;
        privataAudit = new PrivataAudit(true, util.getProps("testApiUrl"));
        try {
            final int status = privataAudit.initialize(util.getProps("testdbKey"), util.getProps("testDbSecret"));
            assertEquals(200, status);
        } catch (final Exception e) {
            exceptionThrown = true;
            System.err.println(e);
        }
        assertEquals(false, exceptionThrown);
    }

    /**
     * Test constructor Error
     */
    @Test(expected = java.lang.Error.class)
    public void testConstructorError() {
        boolean exceptionThrown = false;
        try {
            new PrivataAudit(false);
        } catch (final Exception e) {
            exceptionThrown = true;
            assertEquals("Production environment not available. Please use the sandbox environment.", e.getMessage());
        }
        assertEquals(exceptionThrown, true);
    }

    /**
     * Check that audit objects are being added to the queue
     */

    @Test
    public void testSendQueriesWithoutPersonalData() {
        boolean exceptionThrown = false;

        final JsonArray queries = new JsonArray();
        final JsonObject query = new JsonObject();

        final JsonArray tables = new JsonArray();
        final JsonObject table = new JsonObject();
        table.addProperty("table", "tableName");
        table.add("columns", new JsonArray());
        tables.add(table);

        query.add("tables", tables);
        query.addProperty("action", "Read");
        query.addProperty("timestamp", 1234567);
        query.addProperty("user", "someUser");
        query.addProperty("group", "someGroup");
        query.addProperty("returnedRows", 6);

        queries.add(query);
        try {
            final int res = privataAudit.sendQueries(queries);
            assertEquals(res, 201);
        } catch (final Exception e) {
            exceptionThrown = true;
            System.err.println(e);
        }
        assertEquals(false, exceptionThrown);
    }

    @Test
    public void testSendQueriesWithPersonalData() {
        boolean exceptionThrown = false;
        final JsonArray queries = new JsonArray();

        // build first query
        final JsonObject firstQuery = new JsonObject();
        // build table and columns
        final JsonArray tablesFirstQuery = new JsonArray();
        final JsonObject tableFirstQuery = new JsonObject();
        final List<String> columnsFirstQuery = new ArrayList<String>();
        columnsFirstQuery.add("Region");
        columnsFirstQuery.add("Subregion");
        final Gson gson = new Gson();
        tableFirstQuery.add("columns", gson.toJsonTree(columnsFirstQuery));
        tableFirstQuery.addProperty("table", "Countries");
        tablesFirstQuery.add(tableFirstQuery);
        // add query props
        firstQuery.addProperty("action", "Read");
        firstQuery.addProperty("timestamp", 1234567);
        firstQuery.addProperty("user", "someUser");
        firstQuery.addProperty("group", "someGroup");
        firstQuery.addProperty("returnedRows", 6);
        firstQuery.add("tables", tablesFirstQuery);

        // build second query
        final JsonObject secondQuery = new JsonObject();
        // build table and columns
        final JsonArray tablesSecondQuery = new JsonArray();
        final JsonObject tableSecondQuery = new JsonObject();
        final List<String> columnsSecondQuery = new ArrayList<String>();
        columnsSecondQuery.add("BankAccountCode");
        columnsSecondQuery.add("BankAccountName");
        tableSecondQuery.add("columns", gson.toJsonTree(columnsSecondQuery));
        tableSecondQuery.addProperty("table", "Suppliers");
        tablesSecondQuery.add(tableSecondQuery);
        // add query props
        secondQuery.addProperty("action", "Read");
        secondQuery.addProperty("timestamp", 1234567);
        secondQuery.addProperty("user", "someUser");
        secondQuery.addProperty("group", "someGroup");
        secondQuery.addProperty("returnedRows", 6);
        secondQuery.add("tables", tablesSecondQuery);

        queries.add(firstQuery);
        queries.add(secondQuery);
        try {
            final int res = privataAudit.sendQueries(queries);
            assertEquals(res, 201);
        } catch (final Exception e) {
            exceptionThrown = true;
            System.err.println(e);
        }
        assertEquals(false, exceptionThrown);
    }

    @Test
    public void testSendQueriesWithSQLQuery() {
        boolean exceptionThrown = false;

        final JsonArray queries = new JsonArray();
        final JsonObject query = new JsonObject();
        query.addProperty("sql", "SELECT * FROM Countries");
        query.addProperty("action", "Read");
        query.addProperty("timestamp", 1234567);
        query.addProperty("user", "someUser");
        query.addProperty("group", "someGroup");
        query.addProperty("returnedRows", 6);

        queries.add(query);
        try {
            final int res = privataAudit.sendQueries(queries);
            assertEquals(res, 201);
        } catch (final Exception e) {
            exceptionThrown = true;
            System.err.println(e);
        }
        assertEquals(false, exceptionThrown);
    }
}
