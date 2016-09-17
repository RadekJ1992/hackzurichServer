package hackzurich;

import java.util.List;

/**
 * Bean for last image - it stores image base64 and its labels
 */
public class LastImageBean {
    private String image;
    private List<String> labels;

    public LastImageBean(String image, List<String> labels) {
        this.image = image;
        this.labels = labels;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
