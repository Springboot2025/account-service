package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PostcodeGroup {

    private String postcode;

    private List<SuburbGroup> suburbs;
}
