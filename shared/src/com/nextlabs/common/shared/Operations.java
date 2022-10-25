package com.nextlabs.common.shared;

public enum Operations {
    PROTECT(1),
    SHARE(2),
    REMOVE_USER(3),
    VIEW(4),
    PRINT(5),
    DOWNLOAD(6),
    EDIT_SAVE(7),
    REVOKE(8),
    DECRYPT(9),
    COPY_CONTENT(10),
    CAPTURE_SCREEN(11),
    CLASSIFY(12),
    RESHARE(13),
    DELETE(14),
    OFFLINE(15),
    REMOVE_PROJECT(16),
    VIEW_PARTIAL_DOWNLOAD(17),
    DOWNLOAD_PARTIAL_DOWNLOAD(18),
    OFFLINE_PARTIAL_DOWNLOAD(19),
    HEADER_DOWNLOAD(20),
    SYSTEMBUCKET_DOWNLOAD(21),
    SYSTEMBUCKET_PARTIAL_DOWNLOAD(22),
    UPLOAD_NORMAL(23),
    UPLOAD_PROJECT_SYSBUCKET(24),
    UPLOAD_VIEW(25),
    UPLOAD_EDIT(26),
    UPLOAD_PROJECT(27),
    UPLOAD_ASIS(28),
    ADD_FILE_TO(29),
    ADDED_FROM(30),
    SAVE_AS(31),
    SAVED_FROM(32);

    private int value;

    private Operations(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Operations lookUpByValue(int value) {
        Operations[] ops = Operations.values();
        for (Operations operation : ops) {
            if (operation.value == value) {
                return operation;
            }
        }
        return null;
    }

}
