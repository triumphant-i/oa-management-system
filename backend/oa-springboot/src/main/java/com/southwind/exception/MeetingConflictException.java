package com.southwind.exception;

/**
 * 会议室时间冲突异常
 * 当会议室预约时间与已有预约冲突时抛出
 */
public class MeetingConflictException extends RuntimeException {
    
    private Integer roomId;
    private String roomName;
    private String conflictTime;

    public MeetingConflictException(String message) {
        super(message);
    }

    public MeetingConflictException(Integer roomId, String roomName, String conflictTime) {
        super(String.format("会议室【%s】预约时间冲突：%s", roomName, conflictTime));
        this.roomId = roomId;
        this.roomName = roomName;
        this.conflictTime = conflictTime;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getConflictTime() {
        return conflictTime;
    }
}