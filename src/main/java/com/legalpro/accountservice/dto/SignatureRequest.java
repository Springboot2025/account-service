package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureRequest {
    private String signatureDataUrl;

    /**
     * Optional final rendered HTML snapshot to freeze as the document's
     * content at the moment of signing (the lawyer's builder state is a
     * live JSON draft up to that point; signing locks in a static copy).
     */
    private String content;
}
