package com.legalpro.accountservice.repository.projection;

public interface CaseStatsProjection {

    Long getClosed();
    Long getActive();
    Long getPending();
}

