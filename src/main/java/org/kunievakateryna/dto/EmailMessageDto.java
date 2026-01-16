package org.kunievakateryna.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
public class EmailMessageDto implements Serializable {
    private String recipient;
    private String subject;
    private String body;
}