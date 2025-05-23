package com.example.signaling.service;

import com.example.signaling.model.MessageType;
import com.example.signaling.model.Room;
import com.example.signaling.model.SignalingMessage;
import com.example.signaling.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignalingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    
    public void handleLogin(SignalingMessage message, String sessionId) {
        String userId = message.getFromUserId();
        User user = roomService.createUser(userId, sessionId);
        
        SignalingMessage response = SignalingMessage.builder()
                .type(MessageType.LOGIN_RESPONSE.name())
                .toUserId(userId)
                .data(Map.of("success", true))
                .build();
        
        sendToUser(userId, response);
    }
    
    public void handleLogout(SignalingMessage message) {
        String userId = message.getFromUserId();
        User user = roomService.getUser(userId);
        
        if (user != null) {
            roomService.removeUserFromAllRooms(user);
            roomService.removeUser(userId);
            
            SignalingMessage response = SignalingMessage.builder()
                    .type(MessageType.LOGOUT_RESPONSE.name())
                    .toUserId(userId)
                    .data(Map.of("success", true))
                    .build();
            
            sendToUser(userId, response);
        }
    }
    
    public void handleEnterRoom(SignalingMessage message) {
        String userId = message.getFromUserId();
        String roomId = message.getRoomId();
        User user = roomService.getUser(userId);
        
        if (user != null && roomId != null) {
            Room room = roomService.addUserToRoom(roomId, user);
            
            // Create response with room info
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", roomId);
            roomInfo.put("userCount", room.getUserCount());
            roomInfo.put("users", room.getUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            
            SignalingMessage response = SignalingMessage.builder()
                    .type(MessageType.ENTER_ROOM_RESPONSE.name())
                    .toUserId(userId)
                    .roomId(roomId)
                    .data(Map.of("success", true, "roomInfo", roomInfo))
                    .build();
            
            sendToUser(userId, response);
            
            // Broadcast to other users in the room
            broadcastUserEnterRoom(user, roomId);
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
        messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message);
    }
} 