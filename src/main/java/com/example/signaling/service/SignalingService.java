package com.example.signaling.service;

import com.example.signaling.model.MessageType;
import com.example.signaling.model.Room;
import com.example.signaling.model.SignalingMessage;
import com.example.signaling.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    
    public void handleLogin(SignalingMessage message, String sessionId) {
        log.info("로그인 요청 처리 시작: userId={}, sessionId={}", message.getFromUserId(), sessionId);
        
        if (message.getFromUserId() == null || message.getFromUserId().isEmpty()) {
            log.error("로그인 실패: fromUserId가 null 또는 비어 있음, sessionId={}", sessionId);
            sendErrorToSession(sessionId, "로그인을 위해 사용자 ID가 필요합니다");
            return;
        }
        
        String userId = message.getFromUserId();
        
        try {
            log.debug("사용자 생성 시도: userId={}, sessionId={}", userId, sessionId);
            User user = roomService.createUser(userId, sessionId);
            log.info("사용자 생성 성공: {}", user);
            
            SignalingMessage response = SignalingMessage.builder()
                    .type(MessageType.LOGIN_RESPONSE.name())
                    .toUserId(userId)
                    .data(Map.of("success", true))
                    .build();
            
            log.debug("로그인 응답 메시지 생성: {}", response);
            sendToUser(userId, response);
            log.info("로그인 응답 전송 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("로그인 처리 중 서비스 예외 발생: userId=" + userId + ", sessionId=" + sessionId, e);
            sendErrorToSession(sessionId, "로그인 실패: " + e.getMessage());
        }
    }
    
    public void handleLogout(SignalingMessage message) {
        String userId = message.getFromUserId();
        log.info("로그아웃 요청 처리 시작: userId={}", userId);
        
        if (userId == null || userId.isEmpty()) {
            log.error("로그아웃 실패: fromUserId가 null 또는 비어 있음");
            return;
        }
        
        try {
            User user = roomService.getUser(userId);
            
            if (user != null) {
                log.debug("사용자 찾음: {}, 모든 방에서 제거 시작", user);
                roomService.removeUserFromAllRooms(user);
                roomService.removeUser(userId);
                log.info("사용자 제거 완료: userId={}", userId);
                
                SignalingMessage response = SignalingMessage.builder()
                        .type(MessageType.LOGOUT_RESPONSE.name())
                        .toUserId(userId)
                        .data(Map.of("success", true))
                        .build();
                
                log.debug("로그아웃 응답 메시지 생성: {}", response);
                sendToUser(userId, response);
                log.info("로그아웃 응답 전송 완료: userId={}", userId);
            } else {
                log.warn("로그아웃 실패: 사용자를 찾을 수 없음, userId={}", userId);
            }
        } catch (Exception e) {
            log.error("로그아웃 처리 중 예외 발생: userId=" + userId, e);
        }
    }
    
    public void handleEnterRoom(SignalingMessage message) {
        String userId = message.getFromUserId();
        String roomId = message.getRoomId();
        log.info("방 입장 요청 처리 시작: userId={}, roomId={}", userId, roomId);
        
        if (userId == null || userId.isEmpty()) {
            log.error("방 입장 실패: fromUserId가 null 또는 비어 있음");
            return;
        }
        
        if (roomId == null || roomId.isEmpty()) {
            log.error("방 입장 실패: roomId가 null 또는 비어 있음, userId={}", userId);
            sendError(userId, "방에 입장하기 위해 roomId가 필요합니다");
            return;
        }
        
        try {
            User user = roomService.getUser(userId);
            
            if (user != null) {
                log.debug("사용자 찾음: {}, 방에 추가 시작: {}", user, roomId);
                Room room = roomService.addUserToRoom(roomId, user);
                log.info("사용자 방 입장 완료: userId={}, roomId={}, 현재 참가자 수={}", 
                        userId, roomId, room.getUserCount());
                
                // 방 정보로 응답 생성
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", roomId);
                roomInfo.put("userCount", room.getUserCount());
                roomInfo.put("users", room.getUsers().stream()
                        .map(User::getUserId)
                        .collect(Collectors.toList()));
                
                log.debug("방 정보: {}", roomInfo);
                
                SignalingMessage response = SignalingMessage.builder()
                        .type(MessageType.ENTER_ROOM_RESPONSE.name())
                        .toUserId(userId)
                        .roomId(roomId)
                        .data(Map.of("success", true, "roomInfo", roomInfo))
                        .build();
                
                log.debug("방 입장 응답 메시지 생성: {}", response);
                sendToUser(userId, response);
                log.info("방 입장 응답 전송 완료: userId={}, roomId={}", userId, roomId);
                
                // 방의 다른 사용자들에게 브로드캐스트
                log.debug("방 입장 브로드캐스트 시작: userId={}, roomId={}", userId, roomId);
                broadcastUserEnterRoom(user, roomId);
            } else {
                log.error("방 입장 실패: 사용자를 찾을 수 없음, userId={}", userId);
            }
        } catch (Exception e) {
            log.error("방 입장 처리 중 예외 발생: userId=" + userId + ", roomId=" + roomId, e);
            sendError(userId, "방 입장 실패: " + e.getMessage());
        }
    }
    
    public void handleLeaveRoom(SignalingMessage message) {
        String userId = message.getFromUserId();
        String roomId = message.getRoomId();
        User user = roomService.getUser(userId);
        
        if (user != null && roomId != null) {
            roomService.removeUserFromRoom(roomId, user);
            
            SignalingMessage response = SignalingMessage.builder()
                    .type(MessageType.LEAVE_ROOM_RESPONSE.name())
                    .toUserId(userId)
                    .roomId(roomId)
                    .data(Map.of("success", true))
                    .build();
            
            sendToUser(userId, response);
            
            // Broadcast to other users in the room
            broadcastUserLeaveRoom(user, roomId);
        }
    }
    
    public void handleP2PSignaling(SignalingMessage message) {
        String toUserId = message.getToUserId();
        if (toUserId != null) {
            sendToUser(toUserId, message);
        }
    }
    
    private void broadcastUserEnterRoom(User user, String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            SignalingMessage broadcastMessage = SignalingMessage.builder()
                    .type(MessageType.BROADCAST_ENTER_ROOM.name())
                    .fromUserId(user.getUserId())
                    .roomId(roomId)
                    .data(Map.of("userId", user.getUserId()))
                    .build();
            
            room.getUsers().stream()
                    .filter(u -> !u.getUserId().equals(user.getUserId()))
                    .forEach(u -> sendToUser(u.getUserId(), broadcastMessage));
        }
    }
    
    private void broadcastUserLeaveRoom(User user, String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            SignalingMessage broadcastMessage = SignalingMessage.builder()
                    .type(MessageType.BROADCAST_LEAVE_ROOM.name())
                    .fromUserId(user.getUserId())
                    .roomId(roomId)
                    .data(Map.of("userId", user.getUserId()))
                    .build();
            
            room.getUsers().forEach(u -> sendToUser(u.getUserId(), broadcastMessage));
        }
    }
    
    private void sendToUser(String userId, SignalingMessage message) {
        log.debug("사용자에게 메시지 전송 시작: userId={}, 메시지 타입={}", userId, message.getType());
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message);
            log.debug("사용자에게 메시지 전송 완료: userId={}, 메시지 타입={}", userId, message.getType());
            
            // 추가 디버깅: 메시지 경로 출력
            log.debug("전송 경로: /user/{}/queue/messages", userId);
        } catch (Exception e) {
            log.error("사용자에게 메시지 전송 중 예외 발생: userId=" + userId + ", 메시지 타입=" + message.getType(), e);
        }
    }
    
    private void sendError(String userId, String errorMessage) {
        log.error("사용자에게 오류 전송: userId={}, message={}", userId, errorMessage);
        SignalingMessage errorResponse = SignalingMessage.builder()
                .type(MessageType.ERROR.name())
                .toUserId(userId)
                .data(Map.of("message", errorMessage))
                .build();
        
        sendToUser(userId, errorResponse);
    }
    
    private void sendErrorToSession(String sessionId, String errorMessage) {
        log.error("세션에 오류 전송: sessionId={}, message={}", sessionId, errorMessage);
        try {
            SignalingMessage errorResponse = SignalingMessage.builder()
                    .type(MessageType.ERROR.name())
                    .data(Map.of("message", errorMessage))
                    .build();
            
            messagingTemplate.convertAndSend("/queue/errors/" + sessionId, errorResponse);
            log.debug("세션에 오류 메시지 전송 완료: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("세션에 오류 메시지 전송 중 예외 발생: sessionId=" + sessionId, e);
        }
    }
    
    // 디버깅을 위한 에코 메시지 처리
    public void sendEchoResponse(SignalingMessage message, String sessionId) {
        log.info("에코 응답 전송 시작: sessionId={}", sessionId);
        
        try {
            // 원본 메시지에 에코 표시 추가
            Map<String, Object> echoData = new HashMap<>();
            echoData.put("original", message.getData());
            echoData.put("echo", true);
            echoData.put("timestamp", System.currentTimeMillis());
            
            SignalingMessage echoResponse = SignalingMessage.builder()
                    .type("ECHO_RESPONSE")
                    .fromUserId("server")
                    .toUserId(message.getFromUserId())
                    .data(echoData)
                    .build();
            
            // 다양한 경로로 응답 전송 시도
            String userId = message.getFromUserId();
            if (userId != null && !userId.isEmpty()) {
                log.debug("사용자 ID 기반 에코 응답 전송: userId={}", userId);
                sendToUser(userId, echoResponse);
            }
            
            log.debug("세션 ID 기반 에코 응답 전송: sessionId={}", sessionId);
            messagingTemplate.convertAndSend("/queue/echo/" + sessionId, echoResponse);
            
            // 범용 경로 시도
            log.debug("범용 에코 경로로 응답 전송");
            messagingTemplate.convertAndSend("/topic/echo", echoResponse);
            
            log.info("에코 응답 전송 완료");
        } catch (Exception e) {
            log.error("에코 응답 전송 중 예외 발생: sessionId=" + sessionId, e);
        }
    }
} 