package com.nextlabs.rms.cc.pojos;

public class ControlCenterSearchCriteria {

    private ControlCenterCriteria criteria;

    public ControlCenterCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(ControlCenterCriteria criteria) {
        this.criteria = criteria;
    }

    public ControlCenterSearchCriteria() {
        this.criteria = new ControlCenterCriteria();
    }

    public static class ControlCenterCriteria {

        private SearchField[] fields;
        private SortField[] sortFields;
        private Long[] columns;
        private int pageNo;
        private int pageSize;

        public ControlCenterCriteria() {
            this.pageNo = 0;
            this.pageSize = 65535;
        }

        public SearchField[] getFields() {
            return fields;
        }

        public void setFields(SearchField[] fields) {
            this.fields = fields;
        }

        public SortField[] getSortFields() {
            return sortFields;
        }

        public void setSortFields(SortField[] sortFields) {
            this.sortFields = sortFields;
        }

        public Long[] getColumns() {
            return columns;
        }

        public void setColumns(Long[] columns) {
            this.columns = columns;
        }

        public int getPageNo() {
            return pageNo;
        }

        public void setPageNo(int pageNo) {
            this.pageNo = pageNo;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public static class SearchField {

            private String field;
            private String type;
            private Value value;
            private String nestedField;

            public String getNestedField() {
                return nestedField;
            }

            public void setNestedField(String nestedField) {
                this.nestedField = nestedField;
            }

            public String getField() {
                return field;
            }

            public void setField(String field) {
                this.field = field;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Value getValue() {
                return value;
            }

            public void setValue(Value value) {
                this.value = value;
            }

            public static class Value {

                private String type;
                private String[] fields;

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String[] getFields() {
                    return fields;
                }

                public void setFields(String[] fields) {
                    this.fields = fields;
                }

            }

            public static class TextSearchValue extends Value {

                private String value;

                public String getValue() {
                    return value;
                }

                public void setValue(String value) {
                    this.value = value;
                }
            }

            public static class StringSearchValue extends Value {

                private String[] value;

                public String[] getValue() {
                    return value;
                }

                public void setValue(String[] value) {
                    this.value = value;
                }

            }
        }

        public static class SortField {

            private String field;
            private String order;

            public SortField(String field) {
                if (field.indexOf('-') == 0) {
                    this.field = field.substring(1);
                    this.order = "DESC";
                } else {
                    this.field = field;
                    this.order = "ASC";
                }
            }

            public String getField() {
                return field;
            }

            public void setField(String field) {
                this.field = field;
            }

            public String getOrder() {
                return order;
            }

            public void setOrder(String order) {
                this.order = order;
            }
        }

    }
}
