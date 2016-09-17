package hackzurich;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Bean for last image - it stores image base64 and its labels
 */
@Builder@Data
public class LastImageBean {
    private String image;
    private List<String> labels;
}
