package com.example.signaling.controller;

import com.example.signaling.model.Room;
import com.example.signaling.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomService roomService;
    
    @GetMapping
    public ResponseEntity<Collection<Room>> getAllRooms() {
        Collection<Room> rooms = roomService.getAllRooms();
        // Remove sensitive information like sessionIds
        rooms.forEach(room -> {
            room.getUsers().forEach(user -> user.setSessionId(null));
        });
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoomById(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            // Remove sensitive information like sessionIds
            room.getUsers().forEach(user -> user.setSessionId(null));
            return ResponseEntity.ok(room);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/{roomId}/users")
    public ResponseEntity<Collection<String>> getUsersInRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            Collection<String> userIds = room.getUsers().stream()
                    .map(user -> user.getUserId())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userIds);
        }
        return ResponseEntity.notFound().build();
    }
} 