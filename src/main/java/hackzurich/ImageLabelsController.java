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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Main Controller of the application
 *
 */
@RestController
public class ImageLabelsController { //TODO rename it somehow

    private Vision vision;

    /**
     * ResourceLoader to get to json with credentials and test image
     */
    @Autowired
    private ResourceLoader resourceLoader;

    private static final String APPLICATION_NAME = "Hack Zurich 2016 1019";

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
    public List<EntityAnnotation> upload(@RequestParam(value="imageBase64") String imageBase64) throws IOException {
        if (vision == null) {
            vision = getVisionService();
        }
        //TODO fix base64 decoding now it throws "illegal character 20"
        //Image img  = new Image().encodeContent(Base64.getDecoder().decode());
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
        return response.getLabelAnnotations();
    }
}
