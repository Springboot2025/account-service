package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LawyerSearchGroupedResponse {

    private List<StateGroup> states;

    private long totalElements;   // total states
    private int totalPages;
}
