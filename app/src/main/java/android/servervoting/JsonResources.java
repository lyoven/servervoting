package android.servervoting;

import android.content.res.Resources;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonResources {

    private Resources res;

    public JsonResources(Resources res) {
        this.res = res;
    }

    public List<Map<String, String>> loadArray(int resourceId) throws IOException {
        List<Map<String, String>> array = new ArrayList<>();
        InputStream is = this.res.openRawResource(resourceId);
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        reader.beginArray();
        while (reader.hasNext()) {
            array.add(readObject(reader));
        }
        reader.endArray();
        try {
            reader.close();
        } catch (IOException e) {
            Log.e("closing array reader", e.getMessage());
        }
        return array;
    }

    public Map<String, Map<String, String>> loadObject(int resourceId) throws IOException {
        Map<String, Map<String, String>> obj = new HashMap<>();
        InputStream is = this.res.openRawResource(resourceId);
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            obj.put(key, readObject(reader));
        }
        reader.endObject();
        try {
            reader.close();
        } catch (IOException e) {
            Log.e("closing object reader", e.getMessage());
        }
        return obj;
    }

    private Map<String, String> readObject(JsonReader reader) throws IOException {
        Map<String, String> map = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            String value = reader.nextString();
            map.put(key, value);
        }
        reader.endObject();
        return map;
    }

}
