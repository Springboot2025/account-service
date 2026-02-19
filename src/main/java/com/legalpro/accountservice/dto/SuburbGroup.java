package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SuburbGroup {

    private String suburb;

    private List<LawyerDto> lawyers;
}
