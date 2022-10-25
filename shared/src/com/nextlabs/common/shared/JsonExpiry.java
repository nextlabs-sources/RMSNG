package com.nextlabs.common.shared;

import java.io.Serializable;

public class JsonExpiry implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer option;
    private Long startDate;
    private Long endDate;
    private JsonRelativeDay relativeDay;

    public JsonExpiry() {
    }

    public JsonExpiry(Integer option) {
        this.option = option;
    }

    public Integer getOption() {
        return option;
    }

    public void setOption(Integer option) {
        this.option = option;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public JsonRelativeDay getRelativeDay() {
        return relativeDay;
    }

    public void setRelativeDay(JsonRelativeDay relativeDay) {
        this.relativeDay = relativeDay;
    }

    public static final class JsonRelativeDay implements Serializable {

        private static final long serialVersionUID = 1L;
        private int year;
        private int month;
        private int week;
        private int day;

        public JsonRelativeDay() {
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getWeek() {
            return week;
        }

        public void setWeek(int week) {
            this.week = week;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }
    }
}
