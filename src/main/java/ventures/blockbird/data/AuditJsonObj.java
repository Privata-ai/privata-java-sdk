package ventures.blockbird.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.HashMultimap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// TODO: Change org.json.simple.JSONArray to import com.google.gson.JsonArray
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AuditJsonObj {

       final static Logger logger = LogManager.getLogger(AuditJsonObj.class);


       private final JSONArray queryArray;
       private int queryCount;

       public AuditJsonObj() {
              this.queryCount = 0;
              this.queryArray = new JSONArray();
       }

       /**
        * Append a query to the list before sending to the API. Get all the details of
        * the query and the list of tables and columns that contain PII
        * piiTablesColumns
        * 
        * @param user
        * @param group
        * @param table
        * @param columns
        * @param action
        * @param date
        * @param row_count
        * @param piiTableColumns List of tables and Columns that contain PII
        */
       public void appendQuery(final String user, final String group, final String table, final String[] columns,
                     final String action, final Date date, final int row_count,
                     final HashMultimap<String, String> piiTableColumns) {
              final Map<String, Object> queries = createJsonQueries(user, group, table, columns, action, date,
                            row_count, piiTableColumns);
              if (!queries.isEmpty()) {
                     this.queryArray.add(queries);
                     this.queryCount++;
              }
              return;
       }

       /**
        * Depreciated as of version 0.1_SNAPSHOT: The API changed to remove the
        * 'queries' key from the json Object. It is now a Json Array object.
        * 
        * @param user
        * @param group
        * @param table
        * @param columns
        * @param action
        * @param date
        * @param row_count
        * @return
        */

       public JSONObject getJsonObj() {
              final JSONObject payload = new JSONObject();
              payload.put("queries", this.queryArray);
              return payload;
       }

       /**
        * Returns an array of queries.
        * 
        * @return
        */

       public JSONArray getJsonArr() {
              return this.queryArray;
       }

       public void clear() {
              this.queryArray.clear();
              this.queryCount = 0;
              return;
       }

       public int getQueryCount() {
              return queryCount;
       }

       /**
        * /** Create a JSON object of the query
        * 
        * @param user
        * @param group
        * @param table
        * @param columns
        * @param action
        * @param date
        * @param row_count
        * @param piiTableColumns
        * @return
        */
       public Map<String, Object> createJsonQueries(final String user, final String group, String table,
                     final String[] columns, final String action, final Date date, final int row_count,
                     final HashMultimap<String, String> piiTableColumns) {
              final int timestamp = (int) getUnixTime(date);
              final Map<String, Object> m = new LinkedHashMap<String, Object>();

              // Blockbird puts all Table and Column names in camelCase.
              // Therefore we add this code to ensure that we are checking correctly:
              table = Character.toLowerCase(table.charAt(0)) + table.substring(1);

              // check if table is PII
              if (piiTableColumns.containsKey(table)) {

                     // add columns
                     final ArrayList<String> columnList = new ArrayList<String>();
                     for (String column : columns) {
                            column = Character.toLowerCase(column.charAt(0)) + column.substring(1);
                            if (piiTableColumns.containsEntry(table, column)) {
                                   columnList.add(column);
                            }
                     }

                     // add tables
                     final JSONArray tablesArray = new JSONArray();
                     final JSONObject tablesObj = new JSONObject();
                     tablesObj.put("table", table);
                     tablesObj.put("columns", columnList);
                     tablesArray.add(tablesObj);

                     // combine it all

                     m.put("tables", tablesArray);
                     m.put("user", user);
                     m.put("group", group);
                     m.put("timestamp", timestamp);
                     m.put("action", action);
                     m.put("returnedRows", row_count);
              }
              return m;
       }
       /*
        * Return UNIX time
        */

       private long getUnixTime(final Date date) {
              final long unixTime = date.getTime() / 1000;
              return unixTime;
       }
}