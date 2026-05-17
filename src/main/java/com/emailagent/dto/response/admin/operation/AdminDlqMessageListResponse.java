package com.emailagent.dto.response.admin.operation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class AdminDlqMessageListResponse extends BaseResponse {

    @JsonProperty("requested_count")
    private final int requestedCount;

    @JsonProperty("returned_count")
    private final int returnedCount;

    private final List<MessageItem> messages;

    public AdminDlqMessageListResponse(int requestedCount, List<MessageItem> messages) {
        this.requestedCount = requestedCount;
        this.returnedCount = messages.size();
        this.messages = messages;
    }

    @Getter
    public static class MessageItem {

        private final int index;

        private final String exchange;

        @JsonProperty("routing_key")
        private final String routingKey;

        private final boolean redelivered;

        @JsonProperty("message_count")
        private final int messageCount;

        @JsonProperty("payload_bytes")
        private final int payloadBytes;

        @JsonProperty("payload_encoding")
        private final String payloadEncoding;

        @JsonProperty("payload_preview")
        private final String payloadPreview;

        @JsonProperty("message_id")
        private final String messageId;

        @JsonProperty("content_type")
        private final String contentType;

        private final String timestamp;

        private final Map<String, String> headers;

        public MessageItem(
                int index,
                String exchange,
                String routingKey,
                boolean redelivered,
                int messageCount,
                int payloadBytes,
                String payloadEncoding,
                String payloadPreview,
                String messageId,
                String contentType,
                String timestamp,
                Map<String, String> headers
        ) {
            this.index = index;
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.redelivered = redelivered;
            this.messageCount = messageCount;
            this.payloadBytes = payloadBytes;
            this.payloadEncoding = payloadEncoding;
            this.payloadPreview = payloadPreview;
            this.messageId = messageId;
            this.contentType = contentType;
            this.timestamp = timestamp;
            this.headers = headers;
        }
    }
}
