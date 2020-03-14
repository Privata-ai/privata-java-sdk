package ventures.blockbird.data;

import java.util.Date;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import ventures.blockbird.util.PropsUtil;

/**
 * Unit test for simple App.
 */
public class BlockbirdAuditTests {
    private static BlockbirdAudit bbAudit;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    @BeforeClass
    public static void initBlockbirdAudit()
    {
        PropsUtil props = new PropsUtil("/test.local.properties");

        bbAudit = BlockbirdAudit.getInstance(
            props.getProps("blockbirdAuditTestUrl"),
            props.getProps("blockbirdAuditTestDbKey"),
            props.getProps("blockbirdAuditTestDbSecret")
            );
    }

    /**
     * Check that audit objects are being added to the queue
     */

    @Test
    public void testAddQuery() {
        int numberOfQueries = 5;
        String[] columns = new String[3];
        for (int row_count=0;row_count<numberOfQueries;row_count++){        
            columns[0] = "familyName";
            columns[1] = "lastname";
            columns[2] = "address";
            // must be a table name and column name that is being tracked!
            bbAudit.addQuery("peter", "group", "familyName", columns, "Read", new Date(), row_count);
        }
        assertEquals(numberOfQueries, bbAudit.getQueryCount());
    }

    /**`
     * Check that it connects to the API correctly
     */

    @Test
    public void testRun() {
        int numberOfQueries = 5;
        String[] columns = new String[3];
        for (int row_count=0;row_count<numberOfQueries;row_count++){        
            columns[0] = "familyName";
            columns[1] = "givenName";
            columns[2] = "address";
            bbAudit.addQuery("mary", "group_db2", "personName", columns, "Read", new Date(), 10);
        }
        bbAudit.run();
        // no response so nothing to check
    }
}
