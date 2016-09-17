package hackzurich;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Util providing access to Thesaurus API
 */
public class ThesaurusUtil {

    public final static String THESAURUS_PREFIX = "http://words.bighugelabs.com/api/1/3d93f8e9f22e16abc61754d71fbddf4d/";

    public final static String THESAURUS_SUFFIX = "/json";

    private ThesaurusUtil() {};

    /**
     * Returns the list of provided word's synonyms
     */
    public static List<String> getSynonyms(String word) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(THESAURUS_PREFIX + word + THESAURUS_SUFFIX);

        try {
            // Execute HTTP Get Request
            HttpResponse response = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            JSONTokener tokener = new JSONTokener(json);
            JSONArray finalResult = new JSONArray(tokener);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < finalResult.length(); i++) {
                result.add(((String) finalResult.get(i)).toLowerCase());
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * mock for {{@link ThesaurusUtil#getSynonyms(String)}} so we don't
     * waste API requests (each word is a request and there is a limit of 1k requests/month)
     */
    public static List<String> getSynonymsMock(String word) {
        return Arrays.asList("fire", "violence", "whatever");
    }
}
