package com.closiq.booking.web.dto;

import lombok.Value;

import java.util.UUID;

/** Customer return request — pickup date/window are assigned by the backend. */
@Value
public class ReturnRequestRequest {

    UUID addressId;
}
