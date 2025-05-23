package com.example.signaling.event;

import com.example.signaling.model.User;
import com.example.signaling.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final RoomService roomService;
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        User user = roomService.getUserBySessionId(sessionId);
        
        if (user != null) {
            log.info("User disconnected: {}", user.getUserId());
            roomService.removeUserFromAllRooms(user);
            roomService.removeUser(user.getUserId());
        }
    }
} 