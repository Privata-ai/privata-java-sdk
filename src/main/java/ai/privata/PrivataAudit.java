package ai.privata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.privata.auth.FirebaseAuth;

/**
 * Privata.ai audit SDK
 * 
 * Sends query information from an application to the data-api
 *
 */
public class PrivataAudit {
        private String apiUrl;
        private String dbKey;

        private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
        private FirebaseAuth firebaseAuth;
        private HashMultimap<String, String> piiTableColumns;

        final static Logger logger = LogManager.getLogger(PrivataAudit.class);

        /**
         * This is a class to handle the Privata.ai Logs and send to the API
         * 
         * @param sandbox option to run in sandbox mode
         * @param apiUrl  the URL of the API
         */
        public PrivataAudit(boolean sandbox, String apiUrl) {
                if (!sandbox) {
                        throw new Error("Production environment not available. Please use the sandbox environment.");
                }
                this.firebaseAuth = new FirebaseAuth(sandbox);
                this.apiUrl = apiUrl;
        }

        /**
         * This is a class to handle the Privata.ai Logs and send to the API
         * 
         */
        public PrivataAudit() {
                this(false, "https://api-sandbox.privata.ai");
        }

        /**
         * This is a class to handle the Privata.ai Logs and send to the API
         * 
         * @param sandbox option to run in sandbox mode
         */
        public PrivataAudit(boolean sandbox) {
                this(sandbox, "hhttps://api-sandbox.privata.ai");
        }

        /**
         * This is a class to handle the Privata.ai Logs and send to the API
         * 
         * @param apiUrl the URL of the API
         */
        public PrivataAudit(String apiUrl) {
                this(false, apiUrl);
        }

        /**
         * This is a class to handle the Privata.ai Logs and send to the API
         * 
         * @param dbKey     the ID of the app on Privata.ai
         * @param dbSecret
         */
        public int initialize(String dbKey, String dbSecret) throws Exception {
                try {
                        this.firebaseAuth.auth(dbKey, dbSecret);
                        this.dbKey = dbKey;
                        return 200;
                } catch (Exception e) {
                        logger.error("Could not authenticate database " + dbKey + " with error: " + e);
                        throw e;
                }
        }

        /**
         * This method sends a array of queries to the API
         * 
         * @param queries Array of queries
         */
        public int sendQueries(JsonArray queries) throws Exception {
                logger.info("Auditing the incoming tables and columns.\n");
                try {
                        getTablesWithPersonalData();
                        JsonArray queriesFiltered = getQueriesWithPersonalData(queries);
                        if (queriesFiltered.size() > 0) {
                                send(queriesFiltered);
                        }
                        return 201;
                } catch (Exception e) {
                        logger.error(e);
                        throw e;
                }

        }

        /**
         * This method sends the JSON object to the API
         */
        private int send(JsonArray auditQuery) throws Exception {
                try {
                        URL url = new URL(this.apiUrl + "/databases/" + this.dbKey + "/queries");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        String userIdToken = firebaseAuth.getIdToken();

                        con.setRequestMethod("POST");
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        con.setRequestProperty("authorization", "Bearer " + userIdToken);
                        con.setUseCaches(false);
                        con.setDoOutput(true);
                        con.setDoInput(true);

                        JsonArray jsonArr = auditQuery;
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
                        }
                        in.close();
                        return responseCode;
                } catch (IOException e) {
                        String text = "At " + new Date() + " there was an error connecting : " + e;
                        logger.error(text);
                        throw e;

                }
        }

        /**
         * This method gets the list of tables and columns that contain personal data
         * 
         * @return some sort of HashMultiMap/Hashset
         */
        private void getTablesWithPersonalData() {
                try {
                        URL url = new URL(this.apiUrl + "/databases/" + this.dbKey);
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
         * Gets the list of Tables and Columns for the Database and finds all the tables
         * that 'hasPersonData' and all columns that 'isPersonalData'.
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
        private JsonArray getQueriesWithPersonalData(JsonArray queries) throws Exception {
                try {
                        JsonArray queriesWithPersonalData = new JsonArray();
                        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
                                JsonObject query = queries.get(queryIndex).getAsJsonObject();
                                if (query.has("sql")) {
                                        queriesWithPersonalData.add(query);
                                } else {
                                        boolean isPersonalData = false;
                                        JsonArray tablesWithPersonalData = new JsonArray();
                                        JsonArray tables = query.get("tables").getAsJsonArray();
                                        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                                                JsonObject table = tables.get(tableIndex).getAsJsonObject();
                                                // Privata.ai puts all Table and Column names in camelCase.
                                                // Therefore we add this code to ensure that we are checking correctly:
                                                String tableName = Character
                                                                .toLowerCase(table.get("table").getAsString().charAt(0))
                                                                + table.get("table").getAsString().substring(1);
                                                // check if table is PII
                                                if (this.piiTableColumns.containsKey(tableName)) {
                                                        List<String> columnsWithPersonalData = new ArrayList<String>();
                                                        Set<String> columnsNames = this.piiTableColumns.get(tableName);
                                                        JsonArray queryColumnsNames = table.get("columns")
                                                                        .getAsJsonArray();
                                                        for (int columnIndex = 0; columnIndex < queryColumnsNames
                                                                        .size(); columnIndex++) {
                                                                // Privata.ai puts all Table and Column names in
                                                                // camelCase.
                                                                // Therefore we add this code to ensure that we are
                                                                // checking correctly:
                                                                String columnName = Character
                                                                                .toLowerCase(queryColumnsNames
                                                                                                .get(columnIndex)
                                                                                                .getAsString()
                                                                                                .charAt(0))
                                                                                + queryColumnsNames.get(columnIndex)
                                                                                                .getAsString()
                                                                                                .substring(1);
                                                                if (columnsNames.contains(columnName)) {
                                                                        columnsWithPersonalData.add(columnName);
                                                                        isPersonalData = true;
                                                                }
                                                        }
                                                        JsonObject tableWithPersonalData = new JsonObject();
                                                        tableWithPersonalData.addProperty("table", tableName);

                                                        Gson gson = new Gson();
                                                        tableWithPersonalData.add("columns",
                                                                        gson.toJsonTree(columnsWithPersonalData));
                                                        tablesWithPersonalData.add(tableWithPersonalData);
                                                }
                                        }

                                        if (isPersonalData) {
                                                JsonObject queryWithPersonalData = new JsonObject();
                                                queryWithPersonalData.add("tables", tablesWithPersonalData);
                                                queryWithPersonalData.addProperty("action",
                                                                query.get("action").getAsString());
                                                queryWithPersonalData.addProperty("timestamp",
                                                                query.get("timestamp").getAsInt());
                                                queryWithPersonalData.addProperty("user",
                                                                query.get("user").getAsString());
                                                queryWithPersonalData.addProperty("group",
                                                                query.get("group").getAsString());
                                                queryWithPersonalData.addProperty("returnedRows",
                                                                query.get("returnedRows").getAsInt());

                                                if (query.has("userIP")) {
                                                        queryWithPersonalData.addProperty("userIP",
                                                                        query.get("userIP").getAsString());
                                                }

                                                queriesWithPersonalData.add(queryWithPersonalData);
                                        }
                                }
                        }
                        return queriesWithPersonalData;
                } catch (Exception e) {
                        logger.error(e);
                        throw e;
                }
        }
}
