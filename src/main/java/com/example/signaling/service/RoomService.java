package com.example.signaling.service;

import com.example.signaling.model.Room;
import com.example.signaling.model.User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUserIdMap = new ConcurrentHashMap<>();
    
    public User createUser(String userId, String sessionId) {
        User user = User.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        users.put(userId, user);
        sessionToUserIdMap.put(sessionId, userId);
        return user;
    }
    
    public User getUser(String userId) {
        return users.get(userId);
    }
    
    public User getUserBySessionId(String sessionId) {
        String userId = sessionToUserIdMap.get(sessionId);
        if (userId != null) {
            return users.get(userId);
        }
        return null;
    }
    
    public void removeUser(String userId) {
        User user = users.get(userId);
        if (user != null) {
            sessionToUserIdMap.remove(user.getSessionId());
            users.remove(userId);
        }
    }
    
    public Room getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> Room.builder().roomId(id).build());
    }
    
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    public Collection<Room> getAllRooms() {
        return rooms.values();
    }
    
    public Room addUserToRoom(String roomId, User user) {
        Room room = getOrCreateRoom(roomId);
        room.addUser(user);
        return room;
    }
    
    public void removeUserFromRoom(String roomId, User user) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.removeUser(user);
            if (room.getUserCount() == 0) {
                rooms.remove(roomId);
            }
        }
    }
    
    public void removeUserFromAllRooms(User user) {
        rooms.values().forEach(room -> room.removeUser(user));
        // Clean up empty rooms
        rooms.entrySet().removeIf(entry -> entry.getValue().getUserCount() == 0);
    }
} 