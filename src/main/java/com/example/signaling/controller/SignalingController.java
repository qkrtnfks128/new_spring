package com.example.signaling.controller;

import com.example.signaling.model.MessageType;
import com.example.signaling.model.SignalingMessage;
import com.example.signaling.service.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignalingController {
    
    private final SignalingService signalingService;
    
    @MessageMapping("/login")
    public void handleLogin(@Payload SignalingMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("로그인 요청 수신: userId={}, sessionId={}, 메시지={}", message.getFromUserId(), sessionId, message);
        
        try {
            signalingService.handleLogin(message, sessionId);
            log.info("로그인 요청 처리 완료: userId={}, sessionId={}", message.getFromUserId(), sessionId);
        } catch (Exception e) {
            log.error("로그인 처리 중 컨트롤러 예외 발생: userId=" + message.getFromUserId(), e);
        }
    }
    
    @MessageMapping("/logout")
    public void handleLogout(@Payload SignalingMessage message) {
        log.info("로그아웃 요청 수신: userId={}, 메시지={}", message.getFromUserId(), message);
        
        try {
            signalingService.handleLogout(message);
            log.info("로그아웃 요청 처리 완료: userId={}", message.getFromUserId());
        } catch (Exception e) {
            log.error("로그아웃 처리 중 예외 발생: userId=" + message.getFromUserId(), e);
        }
    }
    
    @MessageMapping("/room/enter")
    public void handleEnterRoom(@Payload SignalingMessage message) {
        log.info("방 입장 요청 수신: userId={}, roomId={}, 메시지={}", 
                message.getFromUserId(), message.getRoomId(), message);
        
        try {
            signalingService.handleEnterRoom(message);
            log.info("방 입장 요청 처리 완료: userId={}, roomId={}", 
                    message.getFromUserId(), message.getRoomId());
        } catch (Exception e) {
            log.error("방 입장 처리 중 예외 발생: userId=" + message.getFromUserId() + 
                    ", roomId=" + message.getRoomId(), e);
        }
    }
    
    @MessageMapping("/room/leave")
    public void handleLeaveRoom(@Payload SignalingMessage message) {
        log.info("방 퇴장 요청 수신: userId={}, roomId={}, 메시지={}", 
                message.getFromUserId(), message.getRoomId(), message);
        
        try {
            signalingService.handleLeaveRoom(message);
            log.info("방 퇴장 요청 처리 완료: userId={}, roomId={}", 
                    message.getFromUserId(), message.getRoomId());
        } catch (Exception e) {
            log.error("방 퇴장 처리 중 예외 발생: userId=" + message.getFromUserId() + 
                    ", roomId=" + message.getRoomId(), e);
        }
    }
    
    @MessageMapping("/signal")
    public void handleP2PSignaling(@Payload SignalingMessage message) {
        log.debug("P2P 시그널링 메시지 수신: 타입={}, from={}, to={}", 
                message.getType(), message.getFromUserId(), message.getToUserId());
        
        try {
            String type = message.getType();
            if (MessageType.OFFER.name().equals(type) || 
                MessageType.ANSWER.name().equals(type) || 
                MessageType.ICE_CANDIDATE.name().equals(type)) {
                
                signalingService.handleP2PSignaling(message);
                log.debug("P2P 시그널링 처리 완료: 타입={}, from={}, to={}", 
                        message.getType(), message.getFromUserId(), message.getToUserId());
            } else {
                log.warn("지원되지 않는 시그널링 메시지 타입: {}", type);
            }
        } catch (Exception e) {
            log.error("P2P 시그널링 처리 중 예외 발생: 타입=" + message.getType() + 
                    ", from=" + message.getFromUserId() + ", to=" + message.getToUserId(), e);
        }
    }
    
} 