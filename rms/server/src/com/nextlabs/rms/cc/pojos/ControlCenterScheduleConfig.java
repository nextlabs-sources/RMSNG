package com.nextlabs.rms.cc.pojos;

public class ControlCenterScheduleConfig {

    private static final long serialVersionUID = -5976465231934670997L;

    private String startDateTime; // Format - Apr 4, 2016 6:48:00 PM
    private String endDateTime;
    private String recurrenceStartTime;
    private String recurrenceEndTime;
    private long recurrenceDateOfMonth = -1;
    private long recurrenceDayInMonth = -1;
    private boolean sunday;
    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getRecurrenceStartTime() {
        return recurrenceStartTime;
    }

    public void setRecurrenceStartTime(String recurrenceStartTime) {
        this.recurrenceStartTime = recurrenceStartTime;
    }

    public String getRecurrenceEndTime() {
        return recurrenceEndTime;
    }

    public void setRecurrenceEndTime(String recurrenceEndTime) {
        this.recurrenceEndTime = recurrenceEndTime;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public long getRecurrenceDateOfMonth() {
        return recurrenceDateOfMonth;
    }

    public void setRecurrenceDateOfMonth(long recurrenceDateOfMonth) {
        this.recurrenceDateOfMonth = recurrenceDateOfMonth;
    }

    public long getRecurrenceDayInMonth() {
        return recurrenceDayInMonth;
    }

    public void setRecurrenceDayInMonth(long recurrenceDayInMonth) {
        this.recurrenceDayInMonth = recurrenceDayInMonth;
    }

    @Override
    public String toString() {
        return String.format("ControlCenterScheduleConfig [startDateTime=%s, endDateTime=%s, recurrenceStartTime=%s, recurrenceEndTime=%s, recurrenceDateOfMonth=%s, recurrenceDayInMonth=%s, sunday=%s, monday=%s, tuesday=%s, wednesday=%s, thursday=%s, friday=%s, saturday=%s]", startDateTime, endDateTime, recurrenceStartTime, recurrenceEndTime, recurrenceDateOfMonth, recurrenceDayInMonth, sunday, monday, tuesday, wednesday, thursday, friday, saturday);
    }

}
