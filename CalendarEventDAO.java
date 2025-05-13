package com.example.taskmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.taskmanager.models.CalendarEvent;
import com.example.taskmanager.models.Task;

import java.util.ArrayList;
import java.util.List;

public class CalendarEventDAO extends BaseDAO  {
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private TaskDAO taskDAO;

    public CalendarEventDAO(Context context) {
        super(context);
        dbHelper = DatabaseHelper.getInstance(context);
        taskDAO = new TaskDAO(context);
    }

    // Mở kết nối đến database
    public void open() {
        database = dbHelper.getWritableDatabase();
        taskDAO.open();
    }

    // Đóng kết nối đến database
    public void close() {
        taskDAO.close();
        dbHelper.close();
    }
    public void setDatabase(SQLiteDatabase database) {
        this.database = database;
        this.ownDatabase = false; // Không tự quản lý database
    }

    // Tạo sự kiện mới
    public long createEvent(CalendarEvent event) {
        ContentValues values = new ContentValues();
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_USER_ID, event.getUserId());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_TASK_ID, event.getTaskId());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_TITLE, event.getTitle());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_RESOURCE_LINK, event.getResourceLink());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE, event.getEventDate());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME, event.getEventTime());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_IS_COMPLETED, event.isCompleted() ? 1 : 0);

        return database.insert(TaskManagerContract.CalendarEventEntry.TABLE_NAME, null, values);
    }

    // Cập nhật sự kiện
    public int updateEvent(CalendarEvent event) {
        ContentValues values = new ContentValues();
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_TITLE, event.getTitle());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_RESOURCE_LINK, event.getResourceLink());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE, event.getEventDate());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME, event.getEventTime());
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_IS_COMPLETED, event.isCompleted() ? 1 : 0);

        return database.update(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                values,
                TaskManagerContract.CalendarEventEntry._ID + " = ?",
                new String[]{String.valueOf(event.getId())}
        );
    }

    // Đánh dấu sự kiện hoàn thành
    public int markEventAsCompleted(long eventId) {
        ContentValues values = new ContentValues();
        values.put(TaskManagerContract.CalendarEventEntry.COLUMN_IS_COMPLETED, 1);

        return database.update(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                values,
                TaskManagerContract.CalendarEventEntry._ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );
    }

    // Xóa sự kiện
    public int deleteEvent(long eventId) {
        return database.delete(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                TaskManagerContract.CalendarEventEntry._ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );
    }

    // Lấy sự kiện theo ID
    public CalendarEvent getEventById(long id) {
        Cursor cursor = database.query(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                null,
                TaskManagerContract.CalendarEventEntry._ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        CalendarEvent event = null;
        if (cursor != null && cursor.moveToFirst()) {
            event = cursorToEvent(cursor);

            // Lấy task liên quan nếu có
            if (event.getTaskId() != null) {
                Task task = taskDAO.getTaskById(event.getTaskId());
                event.setTask(task);
            }

            cursor.close();
        }

        return event;
    }

    // Lấy danh sách sự kiện theo ngày
    public List<CalendarEvent> getEventsByDate(long userId, String date) {
        List<CalendarEvent> events = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = database.query(
                    TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                    null,
                    TaskManagerContract.CalendarEventEntry.COLUMN_USER_ID + " = ? AND " +
                            TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " = ?",
                    new String[]{String.valueOf(userId), date},
                    null,
                    null,
                    TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CalendarEvent event = cursorToEvent(cursor);

                    // Lấy task liên quan nếu có
                    if (event.getTaskId() != null) {
                        Task task = taskDAO.getTaskById(event.getTaskId());
                        event.setTask(task);
                    }

                    events.add(event);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return events;
    }

    // Lấy danh sách sự kiện trong khoảng thời gian
    public List<CalendarEvent> getEventsByDateRange(long userId, String startDate, String endDate) {
        List<CalendarEvent> events = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(
                    TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                    null,
                    TaskManagerContract.CalendarEventEntry.COLUMN_USER_ID + " = ? AND " +
                            TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " >= ? AND " +
                            TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " <= ?",
                    new String[]{String.valueOf(userId), startDate, endDate},
                    null,
                    null,
                    TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " ASC, " +
                            TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CalendarEvent event = cursorToEvent(cursor);
                    events.add(event);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return events;
    }

    // Lấy danh sách sự kiện của một người dùng
    public List<CalendarEvent> getEventsByUser(long userId) {
        List<CalendarEvent> events = new ArrayList<>();

        Cursor cursor = database.query(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                null,
                TaskManagerContract.CalendarEventEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " ASC, " +
                        TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME + " ASC"
        );

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CalendarEvent event = cursorToEvent(cursor);
                events.add(event);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return events;
    }

    // Lấy danh sách sự kiện liên quan đến một task
    public List<CalendarEvent> getEventsByTask(long taskId) {
        List<CalendarEvent> events = new ArrayList<>();

        Cursor cursor = database.query(
                TaskManagerContract.CalendarEventEntry.TABLE_NAME,
                null,
                TaskManagerContract.CalendarEventEntry.COLUMN_TASK_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null,
                null,
                TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE + " ASC, " +
                        TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME + " ASC"
        );

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CalendarEvent event = cursorToEvent(cursor);
                events.add(event);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return events;
    }

    // Lấy danh sách sự kiện sắp tới (trong vòng 7 ngày)
    public List<CalendarEvent> getUpcomingEvents(long userId) {
        List<CalendarEvent> allEvents = getEventsByUser(userId);
        List<CalendarEvent> upcomingEvents = new ArrayList<>();

        // Lọc các sự kiện sắp tới và chưa hoàn thành
        for (CalendarEvent event : allEvents) {
            if (!event.isCompleted() && !event.isPassed()) {
                upcomingEvents.add(event);
            }
        }

        return upcomingEvents;
    }

    // Chuyển đổi từ Cursor sang đối tượng CalendarEvent
    private CalendarEvent cursorToEvent(Cursor cursor) {
        CalendarEvent event = new CalendarEvent();
        event.setId(cursor.getLong(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry._ID)));
        event.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_USER_ID)));

        int taskIdIndex = cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_TASK_ID);
        if (!cursor.isNull(taskIdIndex)) {
            event.setTaskId(cursor.getLong(taskIdIndex));
        }

        event.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_TITLE)));
        event.setResourceLink(cursor.getString(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_RESOURCE_LINK)));
        event.setEventDate(cursor.getString(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_DATE)));
        event.setEventTime(cursor.getString(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_EVENT_TIME)));
        event.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_IS_COMPLETED)) == 1);
        event.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(TaskManagerContract.CalendarEventEntry.COLUMN_CREATED_AT)));

        return event;
    }
}