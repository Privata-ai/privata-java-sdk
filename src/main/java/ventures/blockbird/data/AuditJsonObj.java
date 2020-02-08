package ventures.blockbird.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AuditJsonObj {

       private JSONArray queryArray;
       private int queryCount;

       public AuditJsonObj() {
              this.queryCount = 0;
              this.queryArray = new JSONArray();
       }

       public void appendQuery(String user, String group, String table, String[] columns,
                     String action, Date date, int row_count) {
              Map<String, Object> queries = createJsonQueries(user, group, table, columns, action, date,
                            row_count);
              this.queryArray.add(queries);
              this.queryCount++;
              return;

       }

       /**
        * Depreciated as of version 0.1_SNAPSHOT: The API changed to remove the 'queries' key from the json
        * Object. It is now a Json Array object. 
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
              JSONObject payload = new JSONObject();
              payload.put("queries", this.queryArray);
              return payload;
       }

       /**
        * Returns an array of queries.
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

       public Map<String, Object> createJsonQueries(String user, String group, String table,
                     String[] columns, String action, Date date, int row_count) {
              int timestamp = (int) getUnixTime(date);

              // add columns
              ArrayList<String> columnList = new ArrayList<String>();
              for (String column : columns) {
                     columnList.add(column);
              }

              // add tables
              JSONArray tablesArray = new JSONArray();
              JSONObject tablesObj = new JSONObject();
              tablesObj.put("table", table);
              tablesObj.put("columns", columnList);
              tablesArray.add(tablesObj);

              // combine it all
              Map<String, Object> m = new LinkedHashMap<String, Object>();
              m.put("tables", tablesArray);
              m.put("user", user);
              m.put("group", group);
              m.put("timestamp", timestamp);
              m.put("action", action);
              m.put("returnedRows", row_count);

              return m;
       }
       /*
        * Return UNIX time
        */

       private long getUnixTime(Date date) {
              long unixTime = date.getTime() / 1000;
              return unixTime;
       }
}