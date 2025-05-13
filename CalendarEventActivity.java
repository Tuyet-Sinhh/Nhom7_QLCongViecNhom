package com.example.taskmanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.taskmanager.R;
import com.example.taskmanager.database.CalendarEventDAO;
import com.example.taskmanager.database.ProjectDAO;
import com.example.taskmanager.database.TaskDAO;
import com.example.taskmanager.models.CalendarEvent;
import com.example.taskmanager.models.Project;
import com.example.taskmanager.models.Task;
import com.example.taskmanager.utils.DateTimeUtils;
import com.example.taskmanager.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarEventActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout tilEventTitle, tilResourceLink, tilEventDate, tilEventTime;
    private TextInputEditText etEventTitle, etResourceLink, etEventDate, etEventTime;
    private Spinner spinnerProject, spinnerTask, spinnerReminderTime;
    private CheckBox checkboxReminder;
    private Button btnSaveEvent;
    private ProgressBar progressBar;

    private CalendarEventDAO calendarEventDAO;
    private ProjectDAO projectDAO;
    private TaskDAO taskDAO;
    private SessionManager sessionManager;

    private List<Project> projectList;
    private Map<Integer, Long> projectIdMap; // Map vị trí trong spinner với ID dự án

    private List<Task> taskList;
    private Map<Integer, Long> taskIdMap; // Map vị trí trong spinner với ID nhiệm vụ

    private CalendarEvent existingEvent; // Null nếu đang tạo sự kiện mới
    private boolean isEditing = false;

    // Giá trị mặc định
    private String selectedDate; // Ngày được chọn khi tạo từ calendar fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_event);

        // Khởi tạo các thành phần giao diện
        initViews();

        // Thiết lập toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Khởi tạo database và session
        calendarEventDAO = new CalendarEventDAO(this);
        projectDAO = new ProjectDAO(this);
        taskDAO = new TaskDAO(this);
        sessionManager = new SessionManager(this);

        // Khởi tạo danh sách dự án và nhiệm vụ
        projectList = new ArrayList<>();
        projectIdMap = new HashMap<>();
        taskList = new ArrayList<>();
        taskIdMap = new HashMap<>();

        // Kiểm tra xem đang tạo mới hay chỉnh sửa sự kiện
        if (getIntent().hasExtra("EVENT_ID")) {
            // Chỉnh sửa sự kiện hiện có
            isEditing = true;
            long eventId = getIntent().getLongExtra("EVENT_ID", -1);
            loadExistingEvent(eventId);
            getSupportActionBar().setTitle(R.string.edit_event);
            btnSaveEvent.setText(R.string.save);
        } else {
            // Tạo sự kiện mới
            getSupportActionBar().setTitle(R.string.new_calendar_event);

            // Kiểm tra xem có ngày được chọn không
            if (getIntent().hasExtra("SELECTED_DATE")) {
                selectedDate = getIntent().getStringExtra("SELECTED_DATE");
            } else {
                selectedDate = DateTimeUtils.getCurrentDate();
            }

            // Thiết lập ngày và giờ mặc định
            etEventDate.setText(DateTimeUtils.formatDisplayDate(selectedDate));

            // Tải danh sách dự án
            loadProjects();
        }

        // Thiết lập spinner nhắc nhở
        setupReminderSpinner();

        // Thiết lập sự kiện cho các view
        setupViewListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilEventTitle = findViewById(R.id.til_event_title);
        tilResourceLink = findViewById(R.id.til_resource_link);
        tilEventDate = findViewById(R.id.til_event_date);
        tilEventTime = findViewById(R.id.til_event_time);

        etEventTitle = findViewById(R.id.et_event_title);
        etResourceLink = findViewById(R.id.et_resource_link);
        etEventDate = findViewById(R.id.et_event_date);
        etEventTime = findViewById(R.id.et_event_time);

        spinnerProject = findViewById(R.id.spinner_project);
        spinnerTask = findViewById(R.id.spinner_task);
        spinnerReminderTime = findViewById(R.id.spinner_reminder_time);

        checkboxReminder = findViewById(R.id.checkbox_reminder);

        btnSaveEvent = findViewById(R.id.btn_save_event);

        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupReminderSpinner() {
        // Tạo mảng thời gian nhắc nhở
        String[] reminderTimes = new String[]{
                getString(R.string.reminder_at_time),
                getString(R.string.reminder_5_minutes),
                getString(R.string.reminder_15_minutes),
                getString(R.string.reminder_30_minutes),
                getString(R.string.reminder_1_hour),
                getString(R.string.reminder_2_hours),
                getString(R.string.reminder_1_day)
        };

        // Tạo adapter cho spinner
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, reminderTimes);
        reminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderTime.setAdapter(reminderAdapter);
    }

    private void setupViewListeners() {
        // Sự kiện cho ngày sự kiện
        etEventDate.setOnClickListener(v -> showDatePickerDialog());

        // Sự kiện cho thời gian sự kiện
        etEventTime.setOnClickListener(v -> showTimePickerDialog());

        // Sự kiện cho checkbox nhắc nhở
        checkboxReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Sự kiện cho spinner dự án
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // > 0 vì có mục "Chọn dự án"
                    Long projectId = projectIdMap.get(position);
                    if (projectId != null) {
                        loadTasksForProject(projectId);
                    }
                } else {
                    // Xóa danh sách nhiệm vụ
                    taskList.clear();
                    taskIdMap.clear();
                    setupTaskSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Không cần xử lý gì ở đây
            }
        });

        // Sự kiện cho nút lưu sự kiện
        btnSaveEvent.setOnClickListener(v -> saveEvent());
    }


    private void showDatePickerDialog() {
        // Tạo đối tượng Calendar với ngày hiện tại
        final Calendar calendar = Calendar.getInstance();

        // Nếu đã có ngày trong EditText, sử dụng ngày đó
        String currentDate = etEventDate.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = displayFormat.parse(currentDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Tạo DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Cập nhật ngày đã chọn vào EditText
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    String formattedDate = DateTimeUtils.formatDisplayDate(DateTimeUtils.formatDate(calendar.getTime()));
                    etEventDate.setText(formattedDate);
                },
                year, month, day);

        // Hiển thị hộp thoại
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        // Tạo đối tượng Calendar với thời gian hiện tại
        final Calendar calendar = Calendar.getInstance();

        // Nếu đã có thời gian trong EditText, sử dụng thời gian đó
        String currentTime = etEventTime.getText().toString();
        if (!currentTime.isEmpty()) {
            Date time = DateTimeUtils.parseTime(currentTime);
            if (time != null) {
                calendar.setTime(time);
            }
        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Tạo TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // Cập nhật thời gian đã chọn vào EditText
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    String formattedTime = DateTimeUtils.formatTime(calendar.getTime());
                    etEventTime.setText(formattedTime);
                },
                hour, minute, true);

        // Hiển thị hộp thoại
        timePickerDialog.show();
    }

    private void loadProjects() {
        showProgress(true);

        new Thread(() -> {
            projectDAO.open();
            List<Project> projects = projectDAO.getProjectsByUser(sessionManager.getUserId());
            projectDAO.close();

            if (projects != null) {
                // Lưu trữ danh sách dự án
                projectList.addAll(projects);

                // Cập nhật UI trên main thread
                runOnUiThread(() -> {
                    showProgress(false);

                    // Thiết lập spinner dự án
                    setupProjectSpinner();
                });
            } else {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(CalendarEventActivity.this, R.string.error_loading_projects, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadTasksForProject(long projectId) {
        showProgress(true);

        new Thread(() -> {
            taskDAO.open();
            List<Task> tasks = taskDAO.getTasksByProject(projectId);
            taskDAO.close();

            if (tasks != null) {
                // Lưu trữ danh sách nhiệm vụ
                taskList.clear();
                taskIdMap.clear();
                taskList.addAll(tasks);

                // Cập nhật UI trên main thread
                runOnUiThread(() -> {
                    showProgress(false);

                    // Thiết lập spinner nhiệm vụ
                    setupTaskSpinner();
                });
            } else {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(CalendarEventActivity.this, R.string.error_loading_tasks, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadExistingEvent(long eventId) {
        showProgress(true);

        new Thread(() -> {
            calendarEventDAO.open();
            existingEvent = calendarEventDAO.getEventById(eventId);
            calendarEventDAO.close();

            if (existingEvent == null) {
                // Không tìm thấy sự kiện
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(CalendarEventActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Tải danh sách dự án
            projectDAO.open();
            List<Project> projects = projectDAO.getProjectsByUser(sessionManager.getUserId());
            projectDAO.close();

            if (projects != null) {
                // Lưu trữ danh sách dự án
                projectList.addAll(projects);

                // Kiểm tra xem sự kiện có liên kết với task không
                Long taskId = existingEvent.getTaskId();
                if (taskId != null) {
                    // Tải danh sách task của dự án tương ứng
                    taskDAO.open();
                    Task task = taskDAO.getTaskById(taskId);
                    if (task != null) {
                        long projectId = task.getProjectId();
                        List<Task> tasks = taskDAO.getTasksByProject(projectId);
                        if (tasks != null) {
                            taskList.addAll(tasks);
                        }
                    }
                    taskDAO.close();
                }

                // Cập nhật UI trên main thread
                runOnUiThread(() -> {
                    showProgress(false);

                    // Điền thông tin sự kiện vào form
                    etEventTitle.setText(existingEvent.getTitle());
                    etResourceLink.setText(existingEvent.getResourceLink());
                    etEventDate.setText(DateTimeUtils.formatDisplayDate(existingEvent.getEventDate()));

                    if (existingEvent.getEventTime() != null && !existingEvent.getEventTime().isEmpty()) {
                        etEventTime.setText(existingEvent.getEventTime());
                    }

                    // Thiết lập spinner dự án và nhiệm vụ
                    setupProjectSpinner();
                    setupTaskSpinner();

                    // Thiết lập project và task được chọn
                    if (taskId != null) {
                        Task task = taskDAO.getTaskById(taskId);
                        if (task != null) {
                            long projectId = task.getProjectId();

                            // Tìm vị trí của project trong spinner
                            for (int i = 0; i < projectList.size(); i++) {
                                if (projectList.get(i).getId() == projectId) {
                                    spinnerProject.setSelection(i + 1); // +1 vì có mục "Chọn dự án"
                                    break;
                                }
                            }

                            // Tìm vị trí của task trong spinner
                            for (int i = 0; i < taskList.size(); i++) {
                                if (taskList.get(i).getId() == taskId) {
                                    spinnerTask.setSelection(i + 1); // +1 vì có mục "Chọn nhiệm vụ"
                                    break;
                                }
                            }
                        }
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(CalendarEventActivity.this, R.string.error_loading_projects, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupProjectSpinner() {
        // Tạo danh sách tên dự án
        List<String> projectNames = new ArrayList<>();
        projectNames.add(getString(R.string.select_project)); // Mục đầu tiên

        // Thêm tên dự án vào danh sách
        for (int i = 0; i < projectList.size(); i++) {
            Project project = projectList.get(i);
            projectNames.add(project.getName());
            projectIdMap.put(i + 1, project.getId()); // +1 vì có mục "Chọn dự án"
        }

        // Tạo adapter cho spinner
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, projectNames);
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);
    }

    private void setupTaskSpinner() {
        // Tạo danh sách tên nhiệm vụ
        List<String> taskNames = new ArrayList<>();
        taskNames.add(getString(R.string.select_task)); // Mục đầu tiên

        // Thêm tên nhiệm vụ vào danh sách
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            taskNames.add(task.getTitle());
            taskIdMap.put(i + 1, task.getId()); // +1 vì có mục "Chọn nhiệm vụ"
        }

        // Tạo adapter cho spinner
        ArrayAdapter<String> taskAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, taskNames);
        taskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTask.setAdapter(taskAdapter);
    }

    private void saveEvent() {
        // Kiểm tra và lấy dữ liệu từ form
        if (!validateForm()) {
            return;
        }

        // Lấy dữ liệu từ form
        String title = etEventTitle.getText().toString().trim();
        String resourceLink = etResourceLink.getText().toString().trim();
        String eventDateDisplay = etEventDate.getText().toString().trim();
        String eventTime = etEventTime.getText().toString().trim();

        // Chuyển đổi định dạng ngày
        String eventDate = DateTimeUtils.parseDisplayDate(eventDateDisplay);

        // Lấy ID nhiệm vụ liên quan (nếu có)
        // Make taskId final so it can be used in the lambda
        final Long taskId;
        int taskPosition = spinnerTask.getSelectedItemPosition();
        if (taskPosition > 0) { // > 0 vì mục đầu tiên là "Chọn nhiệm vụ"
            taskId = taskIdMap.get(taskPosition);
        } else {
            taskId = null;
        }

        // Store reference to existingEvent to make it effectively final
        final CalendarEvent eventToUpdate = existingEvent;
        final boolean isEditingEvent = isEditing;

        // Hiển thị progress
        showProgress(true);

        // Lưu sự kiện vào database
        new Thread(() -> {
            calendarEventDAO.open();

            long result;
            if (isEditingEvent && eventToUpdate != null) {
                // Cập nhật sự kiện hiện có
                eventToUpdate.setTitle(title);
                eventToUpdate.setResourceLink(resourceLink);
                eventToUpdate.setEventDate(eventDate);
                eventToUpdate.setEventTime(eventTime);
                eventToUpdate.setTaskId(taskId);

                // Cập nhật sự kiện
                int updateCount = calendarEventDAO.updateEvent(eventToUpdate);
                result = updateCount > 0 ? eventToUpdate.getId() : -1;
            } else {
                // Tạo sự kiện mới
                CalendarEvent newEvent = new CalendarEvent(
                        sessionManager.getUserId(),
                        taskId,
                        title,
                        resourceLink,
                        eventDate,
                        eventTime
                );

                // Lưu sự kiện
                result = calendarEventDAO.createEvent(newEvent);
            }

            calendarEventDAO.close();

            // Kết quả cuối cùng
            final boolean success = result != -1;

            // Cập nhật UI trên main thread
            runOnUiThread(() -> {
                showProgress(false);

                if (success) {
                    // Thông báo thành công
                    Toast.makeText(CalendarEventActivity.this,
                            isEditingEvent ? R.string.event_updated : R.string.event_created,
                            Toast.LENGTH_SHORT).show();

                    // Đóng màn hình
                    finish();
                } else {
                    // Thông báo lỗi
                    Toast.makeText(CalendarEventActivity.this,
                            isEditingEvent ? R.string.error_updating_event : R.string.error_creating_event,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private boolean validateForm() {
        boolean valid = true;

        // Kiểm tra tiêu đề sự kiện
        if (etEventTitle.getText().toString().trim().isEmpty()) {
            tilEventTitle.setError(getString(R.string.fill_all_fields));
            valid = false;
        } else {
            tilEventTitle.setError(null);
        }

        // Kiểm tra ngày sự kiện
        if (etEventDate.getText().toString().trim().isEmpty()) {
            tilEventDate.setError(getString(R.string.fill_all_fields));
            valid = false;
        } else {
            tilEventDate.setError(null);
        }

        return valid;
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        etEventTitle.setEnabled(!show);
        etResourceLink.setEnabled(!show);
        etEventDate.setEnabled(!show);
        etEventTime.setEnabled(!show);
        spinnerProject.setEnabled(!show);
        spinnerTask.setEnabled(!show);
        checkboxReminder.setEnabled(!show);
        spinnerReminderTime.setEnabled(!show);
        btnSaveEvent.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}