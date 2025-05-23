package com.example.signaling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {
    private String type;
    private String fromUserId;
    private String toUserId;
    private String roomId;
    private Object data;
} 