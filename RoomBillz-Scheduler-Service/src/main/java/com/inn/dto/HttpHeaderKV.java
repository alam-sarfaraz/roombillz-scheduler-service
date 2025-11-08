package com.inn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HttpHeaderKV {
	
	@NotBlank
	private String name;
	@NotBlank
	private String value;
}
