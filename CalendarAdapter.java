package com.example.taskmanager.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.taskmanager.R;
import com.example.taskmanager.models.CalendarEvent;
import com.example.taskmanager.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.List;

public class CalendarAdapter extends BaseAdapter {

    private Context context;
    private List<String> dates;
    private String selectedDate;
    private List<CalendarEvent> events;
    private OnDateClickListener listener;

    // Interface cho sự kiện click vào ngày
    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public CalendarAdapter(Context context, List<String> dates, String selectedDate, OnDateClickListener listener) {
        this.context = context;
        this.dates = dates;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events;
    }

    @Override
    public int getCount() {
        return dates.size();
    }

    @Override
    public String getItem(int position) {
        return dates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
            holder = new ViewHolder();
            holder.tvDate = convertView.findViewById(R.id.tv_date);
            holder.viewEventIndicator = convertView.findViewById(R.id.view_event_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String date = dates.get(position);
        holder.tvDate.setText(date);

        if (date.isEmpty()) {
            // Ngày không thuộc tháng hiện tại
            holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.colorDivider));
            holder.viewEventIndicator.setVisibility(View.GONE);
        } else {
            // Kiểm tra xem ngày có phải là ngày đã chọn không
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(DateTimeUtils.parseDate(selectedDate));
            int selectedDay = calendar.get(Calendar.DAY_OF_MONTH);

            if (Integer.parseInt(date) == selectedDay) {
                // Ngày đã chọn
                holder.tvDate.setBackgroundResource(R.drawable.bg_circle);
                holder.tvDate.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                holder.tvDate.setTypeface(null, Typeface.BOLD);
            } else {
                // Ngày bình thường
                holder.tvDate.setBackgroundResource(0);
                holder.tvDate.setTypeface(null, Typeface.NORMAL);

                // Kiểm tra xem ngày có phải là Chủ nhật không
                if (position % 7 == 0) {
                    // Chủ nhật
                    holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.colorError));
                } else {
                    // Các ngày khác
                    holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
                }
            }

            // Kiểm tra xem ngày có sự kiện không
            boolean hasEvent = false;
            if (events != null && !events.isEmpty()) {
                // Lấy ngày đầy đủ của ô hiện tại
                Calendar cal = (Calendar) calendar.clone();
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date));
                String fullDate = DateTimeUtils.formatDate(cal.getTime());

                // Kiểm tra xem ngày có sự kiện không
                for (CalendarEvent event : events) {
                    if (event.getEventDate().equals(fullDate)) {
                        hasEvent = true;
                        break;
                    }
                }
            }

            // Hiển thị chỉ báo sự kiện nếu có
            holder.viewEventIndicator.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvDate;
        View viewEventIndicator;
    }
}