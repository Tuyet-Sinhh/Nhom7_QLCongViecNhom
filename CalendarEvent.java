package com.example.taskmanager.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarEvent implements Serializable {
    private long id;
    private long userId;
    private Long taskId; // Có thể null nếu sự kiện không liên kết với task
    private String title;
    private String resourceLink;
    private String eventDate;
    private String eventTime;
    private boolean isCompleted;
    private String createdAt;
    private Task task; // Liên kết đến task nếu có

    // Constructor rỗng
    public CalendarEvent() {}

    // Constructor không có ID
    public CalendarEvent(long userId, Long taskId, String title, String resourceLink,
                         String eventDate, String eventTime) {
        this.userId = userId;
        this.taskId = taskId;
        this.title = title;
        this.resourceLink = resourceLink;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.isCompleted = false;
    }

    // Constructor đầy đủ
    public CalendarEvent(long id, long userId, Long taskId, String title, String resourceLink,
                         String eventDate, String eventTime, boolean isCompleted, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.title = title;
        this.resourceLink = resourceLink;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResourceLink() {
        return resourceLink;
    }

    public void setResourceLink(String resourceLink) {
        this.resourceLink = resourceLink;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    // Kiểm tra xem sự kiện có trong ngày được chỉ định hay không
    public boolean isOnDate(String date) {
        return eventDate.equals(date);
    }

    // Kiểm tra xem sự kiện đã qua hay chưa
    public boolean isPassed() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date eventDateTime;

            if (eventTime != null && !eventTime.isEmpty()) {
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                eventDateTime = dateTimeFormat.parse(eventDate + " " + eventTime);
            } else {
                eventDateTime = dateFormat.parse(eventDate);
            }

            Date currentDate = new Date();
            return currentDate.after(eventDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Trả về định dạng hiển thị ngày giờ
    public String getFormattedDateTime() {
        if (eventTime != null && !eventTime.isEmpty()) {
            return eventDate + " " + eventTime;
        } else {
            return eventDate;
        }
    }
}