package com.example.signaling.controller;

import com.example.signaling.model.MessageType;
import com.example.signaling.model.SignalingMessage;
import com.example.signaling.service.SignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SignalingController {
    
    private final SignalingService signalingService;
    
    @MessageMapping("/login")
    public void handleLogin(@Payload SignalingMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        signalingService.handleLogin(message, sessionId);
    }
    
    @MessageMapping("/logout")
    public void handleLogout(@Payload SignalingMessage message) {
        signalingService.handleLogout(message);
    }
    
    @MessageMapping("/room/enter")
    public void handleEnterRoom(@Payload SignalingMessage message) {
        signalingService.handleEnterRoom(message);
    }
    
    @MessageMapping("/room/leave")
    public void handleLeaveRoom(@Payload SignalingMessage message) {
        signalingService.handleLeaveRoom(message);
    }
    
    @MessageMapping("/signal")
    public void handleP2PSignaling(@Payload SignalingMessage message) {
        String type = message.getType();
        if (MessageType.OFFER.name().equals(type) || 
            MessageType.ANSWER.name().equals(type) || 
            MessageType.ICE_CANDIDATE.name().equals(type)) {
            signalingService.handleP2PSignaling(message);
        }
    }
} 