package ventures.blockbird.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

// TODO: Guava adds a large overhead to the package. 2.8MB and entire package is 3.2MB
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

import ventures.blockbird.auth.FireBaseAuth;

/**
 * blockbird.data audit SDK
 * 
 * Sends query information from an application to the data-api
 *
 */
public class BlockbirdAudit extends Thread {

        private AuditJsonObj auditQuery;
        private String apiUrl;
        private String dbKey;
        private static int maxQueryQueueLength = 10;
        private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
        private FireBaseAuth firebaseAuth;
        private HashMultimap<String, String> piiTableColumns;

        final static Logger logger = Logger.getLogger(BlockbirdAudit.class);

        private static BlockbirdAudit instance = null;

        /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         * @param apiUrl   the URL of the API
         * @param dbKey    the key (ID) of the database on blockbird.data
         * @param dbSecret
         */
        protected BlockbirdAudit(String apiUrl, String dbKey, String dbSecret) {
                this.auditQuery = new AuditJsonObj();
                this.firebaseAuth = FireBaseAuth.getInstance();
                this.apiUrl = apiUrl;
                this.dbKey = dbKey;
                try {
                        this.firebaseAuth.auth(dbKey, dbSecret);
                        getTablesWithPersonalData();
                        logger.info("Auditing the following tables and columns:\n" + piiTableColumns.toString());
                } catch (Exception e) {
                        logger.error("Could not authenticate Application " + dbKey + " with error: " + e);
                }
        }

        /**
         * This method will return an instance of the BlockbirdAudit class. If it
         * already exists, it will not create a new instance
         * 
         * @param apiUrl   the URL of the API
         * @param dbKey    the ID of the app on blockbird.data
         * @param dbSecret
         */
        public static BlockbirdAudit getInstance(String apiUrl, String dbKey, String dbSecret) {
                if (instance == null) {
                        instance = new BlockbirdAudit(apiUrl, dbKey, dbSecret);
                }
                return instance;
        }

        /**
         * This method appends a query to the JSON object
         * 
         * @param user    the user that is accessing data on the client application
         * @param group   the client group that the user belongs to
         * @param table   the client database table that the user is accessing
         * @param columns the list of columns on the client database that the user is
         *                accessing
         * @param action  the action that the user is performing on the data [Read,
         *                Write, Delete]
         * @param date    the timestamp of the access
         * @param row_count the number of rows returned by the query
         */
        public void addQuery(String user, String group, String table, String[] columns, String action, Date date,
                        int row_count) {
                auditQuery.appendQuery(user, group, table, columns, action, date, row_count, this.piiTableColumns);
                // if auditQuery has reached maxQueryQueueLength, then send to API
                if (auditQuery.getQueryCount() > maxQueryQueueLength) {
                        run();
                }
                return;
        }

        /**
         * This method returns the number of queries in the JSON Object
         * 
         * @return the number of queries left in the queue
         */
        public int getQueryCount() {
                if (auditQuery == null) {
                        return 0;
                }
                return auditQuery.getQueryCount();
        }

        /**
         * This method sends the JSON object to the API
         * 
         * 
         */
        @Override
        public void run() {
                if (auditQuery.getQueryCount() > 0) {

                        try {
                                URL url = new URL(apiUrl + "/databases/" + dbKey + "/queries");
                                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                                String userIdToken = firebaseAuth.getIdToken();

                                con.setRequestMethod("POST");
                                con.setRequestProperty("User-Agent", USER_AGENT);
                                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                                con.setRequestProperty("authorization", "Bearer " + userIdToken);
                                con.setUseCaches(false);
                                con.setDoOutput(true);
                                con.setDoInput(true);

                                JSONArray jsonArr = auditQuery.getJsonArr();
                                try (OutputStream os = con.getOutputStream()) {
                                        byte[] input = jsonArr.toString().getBytes("utf-8");
                                        os.write(input, 0, input.length);
                                }
                                // Get the response
                                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                String inputLine;
                                StringBuffer response = new StringBuffer();

                                int responseCode = con.getResponseCode();

                                while ((inputLine = in.readLine()) != null) {
                                        response.append(inputLine);
                                }

                                if (responseCode == 400) {
                                        logger.error("API Response code: " + responseCode + " - " + response);
                                } else if (responseCode == 404) {
                                        logger.error("API Response code: " + responseCode + " - " + response);
                                } else if (responseCode == 201) {
                                        logger.info("API Response code: " + responseCode + " - " + response);
                                        auditQuery.clear(); // if everything went smoothly, then empty the query backlog
                                }
                                in.close();

                        } catch (IOException e) {

                                String text = "At " + new Date() + " there was an error connecting : " + e;

                                logger.error(text);

                        }
                }

        }

        /**
         * This method gets the list of tables and columns that contain personal data
         * 
         * @return some sort of HashMultiMap/Hashset
         */

        private void getTablesWithPersonalData() {
                try {
                        URL url = new URL(apiUrl + "/databases/" + this.dbKey);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        String userIdToken = firebaseAuth.getIdToken();

                        con.setRequestMethod("GET");
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        con.setRequestProperty("authorization", "Bearer " + userIdToken);
                        con.setUseCaches(false);
                        con.setDoOutput(true);
                        int responseCode = con.getResponseCode();

                        this.piiTableColumns = HashMultimap.create();

                        // Get the response
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                        }

                        addPiiTablesAndColumnsToHashtable(response.toString());

                        if (responseCode == 400) {
                                logger.error("API Response code: " + responseCode + " - " + response);
                        } else if (responseCode == 404) {
                                logger.error("API Response code: " + responseCode + " - " + response);
                        } else if (responseCode == 201) {
                                logger.info("API Response code: " + responseCode + " - " + response);
                        }
                        in.close();

                } catch (Exception e) {
                        logger.error(e);
                }
                return;
        }

        /**
         * Gets the list of Tables and Columns for the Database and finds all the tables that 'hasPersonData'
         * and all columns that 'isPersonalData'.
         * 
         * @param jsonResponse
         */

        private void addPiiTablesAndColumnsToHashtable(String jsonResponse) {

                JsonParser jsonParser = new JsonParser();
                JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonResponse);

                JsonArray json_tables = (JsonArray) jsonObject.get("tables");
                // Run through array and get the tables that 'hasPersonalData' and
                // then columns that 'isPersonal Data'
                for (int i = 0; i < json_tables.size(); i++) {
                        JsonObject json_table = (JsonObject) json_tables.get(i);
                        boolean hasPersonalData = json_table.get("hasPersonalData").getAsBoolean();
                        if (hasPersonalData) {
                                JsonArray json_columns = json_table.getAsJsonArray("columns");
                                for (int j = 0; j < json_columns.size(); j++) {
                                        String table_name = json_table.get("name").getAsString();
                                        JsonObject json_column = (JsonObject) json_columns.get(j);
                                        boolean isPersonalData = json_column.get("isPersonalData").getAsBoolean();
                                        if (isPersonalData) {
                                                String column_name = json_column.get("name").getAsString();
                                                // add to HashMultiMap
                                                this.piiTableColumns.put(table_name, column_name);
                                        }
                                }
                        }
                }
                return;
        }

        /**
         * There is a chance that the object is destroyed with unsent queries to the
         * API.
         * 
         * This method overides the method for when an object is destroyed and uses the
         * run() method to send the last queries
         * 
         */
        @Override
        protected void finalize() throws Throwable {
                run();
                super.finalize();
        }
}
