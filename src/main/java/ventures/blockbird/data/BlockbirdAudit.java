package ventures.blockbird.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

// TODO: Guava adds a large overhead to the package. 2.8MB and entire package is 3.2MB
import com.google.common.collect.HashMultimap;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
        private boolean sandbox;
        private String apiUrl;
        private String dbId;
        private static int maxQueryQueueLength = 10;
        private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
        private FireBaseAuth firebaseAuth;
        private HashMultimap<String, String> piiTableColumns;

        final static Logger logger = LogManager.getLogger(BlockbirdAudit.class);

        private static BlockbirdAudit instance = null;


        /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         * @param sandbox  option to run in sandbox mode
         * @param apiUrl   the URL of the API
         */
        protected BlockbirdAudit(boolean sandbox, String apiUrl) {
                if(!sandbox) {
                        throw new Error("Production environment not available. Please use the sandbox environment.");
                }
                this.sandbox = sandbox;
                this.apiUrl = apiUrl;
                this.auditQuery = new AuditJsonObj();
        }

        /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         */
        protected BlockbirdAudit() {
                this(false, "https://api-staging.blockbird.ventures");
        }

         /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         * @param sandbox  option to run in sandbox mode
         */
        protected BlockbirdAudit(boolean sandbox) {
                this(sandbox, "https://api-staging.blockbird.ventures");
        }

        /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         * @param apiUrl   the URL of the API
         */
        protected BlockbirdAudit(String apiUrl) {
                this(false, apiUrl);
        }

        /**
         * This is a class to handle the Blockbird Logs and send to the API
         * 
         * @param dbId     the ID of the app on blockbird.data
         * @param dbSecret
         */
        public int initialize(String dbId, String dbSecret) throws Exception {
                try {
                        this.firebaseAuth.auth(dbId, dbSecret);
                        this.dbId = dbId;
                        return 200;
                } catch (Exception e) {
                        logger.error("Could not authenticate Application " + dbId + " with error: " + e);
                        throw e;
                }
        }

        /**
         * This method sends a array of queries to the API
         * 
         * @param queries Array of queries
         */
        public void sendQueries(JsonArray queries) {
                logger.info("Auditing the following tables and columns:\n" + piiTableColumns.toString());

                String idToken = firebaseAuth.getIdToken();

                getTablesWithPersonalData();

                return;
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
                                URL url = new URL(this.apiUrl + "/databases/" + this.dbId + "/queries");
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
                        URL url = new URL(this.apiUrl + "/databases/" + this.dbId);
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
                // clear before addind updated tables/columns with personal data
                this.piiTableColumns.clear(); 

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
         * This method filters the queries submitted that have personal data
         * 
         * @return Array of queries that have personal data
         */

        private void getQueriesWithPersonalData(JsonArray queries) {
                try {
                        JsonArray queriesWithPersonalData;
                        // Blockbird puts all Table and Column names in camelCase.
                        // Therefore we add this code to ensure that we are checking correctly:
                        //String table = Character.toLowerCase(table.charAt(0)) + table.substring(1);

                        for (int queryIndex = 0 ; queryIndex < queries.size(); queryIndex++) {

                                JsonObject query = queries.get(queryIndex).getAsJsonObject();
                                
                                if(query.has("sql")){
                                        queriesWithPersonalData.add(query);
                                }
                                else {
                                        boolean isPersonalData = false;
                                        JsonArray tablesWithPersonalData;
                                        JsonArray tables = query.get("tables").getAsJsonArray();
                                        
                                        for (int tableIndex = 0 ; tableIndex < tables.size(); tableIndex++) {
                                                JsonObject table = tables.get(tableIndex).getAsJsonObject();
                                                String tableName = table.get("table").getAsString();

                                                // check if table is PII
                                                if (this.piiTableColumns.containsKey(tableName)) {
                                                        String[] columnsWithPersonalData;

                                                        Set<String> columns = this.piiTableColumns.get(table);
                                                        JsonArray columnNamesJson = table.get("columns").getAsJsonArray();
                                                        
                                                        for (int columnIndex = 0 ; columnIndex < columnNames.size(); columnIndex++) {
                                                                if (columns.contains(columns.get(tableIndex).getAsString())) {
                                                                        columnsWithPersonalData.add(columnNames[columnIndex]);
                                                                        isPersonalData = true;
                                                                }
                                                        }

                                                        JsonObject tableWithPersonalData = new JsonObject();
                                                        tableWithPersonalData.addProperty("table", tableName);
                                                        tableWithPersonalData.addProperty("columns", columnsWithPersonalData);

                                                        tablesWithPersonalData.add(tableWithPersonalData);
                                                }
                                        }

                                        if (isPersonalData) {
                                                JsonObject queryWithPersonalData = new JsonObject();
                                                queryWithPersonalData.addProperty("tables", tablesWithPersonalData);
                                                queryWithPersonalData.addProperty("action", query.get("action").getAsString());
                                                queryWithPersonalData.addProperty("timestamp", query.get("timestamp").getInt());
                                                queryWithPersonalData.addProperty("user", query.get("user").getAsString());
                                                queryWithPersonalData.addProperty("group", query.get("group").getAsString());
                                                queryWithPersonalData.addProperty("returnedRows", query.get("returnedRows").getInt());

                                                if(query.has("userIP")){
                                                        queryWithPersonalData.addProperty("userIP", query.get("userIP").getAsString());
                                                }

                                                queriesWithPersonalData.add(queryWithPersonalData);
                                        }
                                }
                        }
                        
                } catch (Exception e) {
                        logger.error(e);
                }
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
