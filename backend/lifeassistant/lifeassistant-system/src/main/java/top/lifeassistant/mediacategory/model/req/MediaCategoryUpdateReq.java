package top.lifeassistant.mediacategory.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediaCategoryUpdateReq {

    @NotBlank
    private String name;
}
