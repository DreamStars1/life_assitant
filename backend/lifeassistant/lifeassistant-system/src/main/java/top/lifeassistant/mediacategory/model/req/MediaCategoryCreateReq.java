package top.lifeassistant.mediacategory.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediaCategoryCreateReq {

    @NotBlank
    private String name;
}
