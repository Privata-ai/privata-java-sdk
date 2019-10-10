package ventures.blockbird.data;

import static org.junit.Assert.assertEquals;

import java.util.Date;

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
        PropsUtil props = new PropsUtil("/test.properties");
        System.out.println("Properties "+props.getProps("blockbirdAuditTestUrl"));

        bbAudit = BlockbirdAudit.getInstance(
            props.getProps("blockbirdAuditTestUrl"),
            props.getProps("blockbirdAuditTestAppId"),
            props.getProps("blockbirdAuditTestDbId"),
            props.getProps("blockbirdAuditTestUsername"),
            props.getProps("blockbirdAuditTestPassword")
            );
    }

    @Test
    public void testAddQuery() {
        int numberOfQueries = 5;
        String[] columns = new String[3];
        for (int row_count=0;row_count<numberOfQueries;row_count++){        
            columns[0] = "firstname";
            columns[1] = "lastname";
            columns[2] = "address";
            bbAudit.addQuery("peter", "group", "ClientTable", columns, "Read", new Date(), row_count);
        }
        assertEquals(numberOfQueries, bbAudit.getQueryCount());
    }

    /**
     * Check that it connects to the API correctly
     */

    @Test
    public void testRun() {
        System.out.println("Lenght: "+bbAudit.getQueryCount());
        bbAudit.run();
        // no response so nothing to check
    }


}
