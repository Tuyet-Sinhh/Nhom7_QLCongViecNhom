package com.example.taskmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanager.R;
import com.example.taskmanager.database.CalendarEventDAO;
import com.example.taskmanager.models.CalendarEvent;
import com.example.taskmanager.models.Task;

import java.util.List;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private Context context;
    private List<CalendarEvent> eventList;
    private OnEventClickListener listener;
    private CalendarEventDAO eventDAO;

    // Interface cho sự kiện click vào event
    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public CalendarEventAdapter(Context context, List<CalendarEvent> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.listener = listener;
        this.eventDAO = new CalendarEventDAO(context);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {
        private View viewStatusIndicator;
        private TextView tvEventTime, tvEventTitle, tvEventDescription, tvEventLink;
        private CheckBox checkboxCompleted;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            tvEventTime = itemView.findViewById(R.id.tv_event_time);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventDescription = itemView.findViewById(R.id.tv_event_description);
            tvEventLink = itemView.findViewById(R.id.tv_event_link);
            checkboxCompleted = itemView.findViewById(R.id.checkbox_completed);

            // Thiết lập sự kiện click cho item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEventClick(eventList.get(position));
                }
            });
        }

        public void bind(CalendarEvent event) {
            // Hiển thị thời gian sự kiện
            if (event.getEventTime() != null && !event.getEventTime().isEmpty()) {
                tvEventTime.setVisibility(View.VISIBLE);
                tvEventTime.setText(event.getEventTime());
            } else {
                tvEventTime.setVisibility(View.GONE);
            }

            // Hiển thị tiêu đề sự kiện
            tvEventTitle.setText(event.getTitle());

            // Kiểm tra liên kết với task
            Task task = event.getTask();
            if (task != null) {
                // Hiển thị mô tả từ task
                tvEventDescription.setVisibility(View.VISIBLE);
                tvEventDescription.setText(task.getDescription());

                // Hiển thị indcator với màu sắc tương ứng với trạng thái task
                int colorId;
                switch (task.getStatus()) {
                    case "Hoàn thành":
                        colorId = R.color.colorSuccess;
                        break;
                    case "Đang thực hiện":
                        colorId = R.color.colorPrimary;
                        break;
                    case "Tạm hoãn":
                        colorId = R.color.colorError;
                        break;
                    default:
                        colorId = R.color.colorNeutral;
                        break;
                }
                viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, colorId));
            } else {
                // Sự kiện không liên kết với task
                if (event.isCompleted()) {
                    viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSuccess));
                } else {
                    viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
                }

                // Ẩn mô tả
                tvEventDescription.setVisibility(View.GONE);
            }

            // Hiển thị đường dẫn tài nguyên nếu có
            if (event.getResourceLink() != null && !event.getResourceLink().isEmpty()) {
                tvEventLink.setVisibility(View.VISIBLE);
                tvEventLink.setText(event.getResourceLink());
            } else {
                tvEventLink.setVisibility(View.GONE);
            }

            // Thiết lập trạng thái đã hoàn thành
            checkboxCompleted.setChecked(event.isCompleted());
            checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    // Cập nhật trạng thái hoàn thành
                    event.setCompleted(isChecked);

                    new Thread(() -> {
                        eventDAO.open();
                        eventDAO.updateEvent(event);
                        eventDAO.close();
                    }).start();

                    // Cập nhật màu sắc indicator
                    if (event.getTask() == null) {
                        viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context,
                                isChecked ? R.color.colorSuccess : R.color.colorPrimary));
                    }
                }
            });
        }
    }
}