package ai.privata;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SampleData {
    public JSONObject getJsonObject() {
        // JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader("./src/main/resources/sample-data.json")) {
            // Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONObject sampleData = (JSONObject) obj;

            return sampleData;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}