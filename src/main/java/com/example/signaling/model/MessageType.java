package com.example.signaling.model;

public enum MessageType {
    LOGIN,
    LOGIN_RESPONSE,
    LOGOUT,
    LOGOUT_RESPONSE,
    ENTER_ROOM,
    ENTER_ROOM_RESPONSE,
    BROADCAST_ENTER_ROOM,
    LEAVE_ROOM,
    LEAVE_ROOM_RESPONSE,
    BROADCAST_LEAVE_ROOM,
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    ERROR
} 