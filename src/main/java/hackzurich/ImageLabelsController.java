package hackzurich;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.*;
import com.google.common.collect.ImmutableList;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Controller of the application
 *
 */
@RestController
public class ImageLabelsController {

    Logger log = LoggerFactory.getLogger(ImageLabelsController.class);

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
     * Last uploaded image (currently stored in memory)
     */
    String lastImageBase64 = "";


    /**
     * Map unique photo id -> photo
     */
    Map<Integer, String> photosMap = new HashMap<Integer, String>();

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
     * to try it run in console (change images.jpeg to path to image)
     *
     * (echo -n $(base64 images.jpeg)) | curl -H "Content-Type: application/json" -d @-  http://localhost:8080/upload
     * @param imageBase64 image encoded in base64
     * @return list of possible labels
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String upload(@RequestBody String imageBase64) throws IOException {
        if (mobileId.equals("")) {
            return "mobile ID not set !";
        }
        this.lastImageBase64 = imageBase64;
        List<String> annotationsList = getImageLabels(imageBase64);
        log.info("image labels from Google API: "
                + annotationsList.stream().reduce((x, y) -> x + "," + y));
        if (annotationsList.stream()
                .filter(annotation -> !annotation.contains(" ")) // we don't want complex keywords with multiple words
                // if any of returned labels is keyword or any synonym of these labels is the keyword return true
                // LAZY - if first one matches no other calls to thesaurus API are created
                .anyMatch(annotation -> keywords.contains(annotation)
                                // we create new ArrayList because getSynonyms returns immutable list
                                || new ArrayList<>(getSynonyms(annotation)).removeAll(keywords))) {
            return "Notified user " + notifyUser();
        } else {
            return "User not notified";
        }
    }

    /**
     * Notifies user via FireBase
     */
    public String notifyUser() {
        try  {
            Integer uniquePhotoId = new Random().nextInt();
            photosMap.put(uniquePhotoId, lastImageBase64);
            String fileUrl = "/getLastPhoto/" + uniquePhotoId;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost("https://fcm.googleapis.com/fcm/send");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization","key=AIzaSyATEU5Q4_ILtSJKYA07Gb1OD156akTL9VI");
            StringEntity params =new StringEntity("{ \"data\": {\"body\":\"" + fileUrl + "\" }, " +
                    "\"to\" : \"" +
                    mobileId +
                    "\"}");
            request.setEntity(params);
            log.info(fileUrl);
            org.apache.http.HttpResponse resp  = httpClient.execute(request);
            log.info(resp.toString());
            return fileUrl;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void sendToTopic() {
        try  {
            String fileUrl = "/lastPhoto";
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost("https://fcm.googleapis.com/fcm/send");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization","key=AIzaSyATEU5Q4_ILtSJKYA07Gb1OD156akTL9VI");
            StringEntity params =new StringEntity("{ \"data\": {\"body\":\"" + fileUrl + "\" }, " +
                    "\"to\" : \"/topics/lastPhoto\"}");
            request.setEntity(params);
            log.info("Sending photo to topic lastPhoto");
            org.apache.http.HttpResponse resp  = httpClient.execute(request);
            log.info(resp.toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get word synonyms from thesaurus API via HTTP GET request
     *
     * Usage: http://localhost:8080/getSynonyms?word=WORD
     */
    @RequestMapping(value = "/getSynonyms")
    public List<String> getSynonyms(@RequestParam(value="word") String word) {
        List<String> synonyms = ThesaurusUtil.getSynonyms(word);
        log.info("Synonyms for word " + word + ": " + synonyms.stream().reduce((x,y) -> x + "," + y).get());
        return synonyms;
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

    @RequestMapping(value = "/getLastImage")
    public LastImageBean getLastImage() {
        try {
            return new LastImageBean(lastImageBase64, getImageLabels(lastImageBase64));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/uploadImageAndGetLabels")
    public List<String> uploadImageAndGetLabels(@RequestBody String imageBase64) throws IOException {
        this.lastImageBase64 = imageBase64;
        return getImageLabels(imageBase64);
    }

    /**
     * Downloads list of labels for provided image from Google Vision API
     * @param imageInBase64 image encoded in base64
     * @return list of labels
     */
    private List<String> getImageLabels(String imageInBase64) throws IOException {
        if (vision == null) {
            vision = getVisionService();
        }
        if (imageInBase64.equals("")) {
            return new ArrayList<>();
        }
        Image img  = new Image().encodeContent(Base64.getDecoder().decode(imageInBase64));
        AnnotateImageRequest request =
                new AnnotateImageRequest()
                        .setImage(img)
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
        return response.getLabelAnnotations().stream()
                .map(EntityAnnotation::getDescription).collect(Collectors.toList());
    }

    @ResponseBody
    @RequestMapping(value = "/getLastPhoto/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getLastPhoto(@PathVariable("id") Integer id) throws IOException {
        return Base64.getDecoder().decode(photosMap.get(id));
    }

    @ResponseBody
    @RequestMapping(value = "/lastPhoto", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] lastPhoto() throws IOException {
        return Base64.getDecoder().decode(lastImageBase64);
    }
}
