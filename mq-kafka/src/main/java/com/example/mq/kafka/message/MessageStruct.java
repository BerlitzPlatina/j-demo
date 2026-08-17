package com.example.mq.kafka.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * Test message body for the JSON topic.
 * </p>
 *
 * @author NamHoang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;

    private Integer amount;

    /**
     * Epoch millis stamped by the producer, handy to see the end to end delay in the logs.
     */
    private Long sentAt;
}
