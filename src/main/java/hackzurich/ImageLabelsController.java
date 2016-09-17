package hackzurich;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.*;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main Controller of the application
 *
 */
@RestController
@Slf4j
public class ImageLabelsController { //TODO rename it somehow

    private Vision vision;

    /**
     * ResourceLoader to get to json with credentials and test image
     */
    @Autowired
    private ResourceLoader resourceLoader;

    private static final String APPLICATION_NAME = "Hack Zurich 2016 1019";

    /**
     * Set of keywords
     */
    List<String> keywords = new ArrayList<>();

    /**
     * MobileUserIdentifier
     */
    String mobileId = ""; //TODO store them as <id, keywords> map so we can handle multiple users

    /**
     * Connects to the Vision API using Application Credentials.
     */
    public Vision getVisionService() {
        try {
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            GoogleCredential credential = GoogleCredential.fromStream(
                    //load credentials from json
                    resourceLoader.getResource("classpath:credentials.json").getInputStream())
                    .createScoped(VisionScopes.all());

            return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uploads image via POST method, sends it to Google Vision API and sends back JSON with
     * labels and their scores
     *
     * to try it go to http://localhost:8080/upload?imageBase64=1
     * @param imageBase64 image encoded in base64
     * @return list of possible labels
     */
    @RequestMapping(value = "/upload") //TODO add method = RequestMethod.POST
    public String upload(@RequestParam(value="imageBase64") String imageBase64) throws IOException {
        if (vision == null) {
            vision = getVisionService();
        }

        if (mobileId.equals("")) {
            return "mobile ID not set !";
        }
        //TODO fix base64 decoding now it throws "illegal character 20"
        //Image img  = new Image().encodeContent(Base64.getDecoder().decode(imageBase64.replaceAll("\\", "")));
        AnnotateImageRequest request =
                new AnnotateImageRequest()
                        .setImage(new Image().encodeContent(
                                //upload random image, after fixing the bug use "img" file
                                ByteStreams.toByteArray(
                                        resourceLoader.getResource("classpath:maxresdefault.jpg").getInputStream())))
                        .setFeatures(ImmutableList.of(
                                new Feature()
                                        .setType("LABEL_DETECTION")
                                        .setMaxResults(10)));
        Vision.Images.Annotate annotate =
                vision.images()
                        .annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotate.setDisableGZipContent(true);

        BatchAnnotateImagesResponse batchResponse = annotate.execute();
        assert batchResponse.getResponses().size() == 1;
        AnnotateImageResponse response = batchResponse.getResponses().get(0);
        if (response.getLabelAnnotations() == null) {
            throw new RuntimeException();
        }
        List<EntityAnnotation> annotationsList = response.getLabelAnnotations();

        if (annotationsList.stream()
                .map(EntityAnnotation::getDescription)
                .filter(annotation -> !annotation.contains(" ")) // we don't want complex keywords
                // if any of returned labels is keyword or any synonym of these labels is the keyword return true
                .anyMatch(annotation -> keywords.contains(annotation)
                                // we create new ArrayList because getSynonyms returns immutable list
                                        || new ArrayList<>(getSynonyms(annotation)).removeAll(keywords))) {
            notifyUser();
            return "Notified user";
        } else {
            return "User not notified";
        }
    }

    public void notifyUser() {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpPost request = new HttpPost("https://fcm.googleapis.com/fcm/send");
            request.addHeader("content-type", "application/json");
            // request.addHeader("Accept","application/json");
            request.addHeader("Authorization","key=AIzaSyATEU5Q4_ILtSJKYA07Gb1OD156akTL9VI");

            StringEntity params =new StringEntity("{ \"notification\": {\"body\":\"notification!\" }, " +
                    "\"to\" : \"" +
                    mobileId +
                    "\"}");
            request.setEntity(params);
            httpClient.execute(request);
        }catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    /**
     * Get word synonyms from thesaurus API via HTTP GET request
     */
    @RequestMapping(value = "/thesaurusTest")
    public List<String> getSynonyms(@RequestParam(value="word") String word) {
        return ThesaurusUtil.getSynonymsMock(word); //TODO removeMock
    }

    /**
     * Sets global keywords for image labels and the mobile ID
     *
     * Usage: http://localhost:8080/setMobileIdAndKeywords?keywords=fire,wood,forest&mobileId=MOBILE_ID
     * @param keywords keywords seperated by comma ','
     * @param mobileId mobile identifier
     */
    @RequestMapping(value = "/setMobileIdAndKeywords")
    public void setMobileIdAndKeywords(@RequestParam(value = "mobileId") String mobileId, @RequestParam(value = "keywords") String keywords) {
        log.info("mobileId = " + mobileId);
        this.mobileId = mobileId;
        log.info("keywords = " + keywords);
        this.keywords = Arrays.stream(keywords.split(",")).collect(Collectors.toList());
    }

}
