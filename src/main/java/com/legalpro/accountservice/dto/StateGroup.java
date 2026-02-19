package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StateGroup {

    private String state;

    private long totalLawyers;

    private List<PostcodeGroup> postcodes;
}
