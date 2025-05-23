package com.example.signaling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String roomId;
    
    @Builder.Default
    private Set<User> users = new HashSet<>();
    
    public void addUser(User user) {
        users.add(user);
    }
    
    public void removeUser(User user) {
        users.removeIf(u -> u.getUserId().equals(user.getUserId()));
    }
    
    public int getUserCount() {
        return users.size();
    }
} 