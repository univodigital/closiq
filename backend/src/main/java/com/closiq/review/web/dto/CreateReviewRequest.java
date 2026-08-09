package com.closiq.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.List;

@Value
public class CreateReviewRequest {

    @NotBlank
    String bookingId;

    @NotNull
    @Min(1)
    @Max(5)
    Integer productRating;

    @Min(1)
    @Max(5)
    Integer sellerRating;

    @Size(min = 10, max = 1000)
    String comment;

    @Size(max = 3)
    List<String> photoUploadIds;
}
