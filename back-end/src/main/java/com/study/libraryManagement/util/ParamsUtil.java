package com.study.libraryManagement.util;

public class ParamsUtil {
    public static final Integer BORROW_MAX_COUNT = 1;
    public static final Integer ISBN_LENGTH = 13;
    public static final String LOCAL_PATH = "F:/JavaProgram/java_workspace/study-library-management/photos";
    public static final String SERVER_PATH = "http://localhost:8099/Photos";
    public static final String BOOK_CACHE_PREFIX = "book:detail:isbn:";
    public static final String NULL_CACHE_VALUE = "__NULL__";
    public static final long BOOK_CACHE_MINUTES = 30L;
    public static final long NULL_CACHE_MINUTES = 2L;
    public static final String BOOK_CACHE_LOCK_PREFIX = "lock:book:cache:";
    public static final long BOOK_CACHE_LOCK_SECONDS = 10L;
    public static final long BOOK_CACHE_RETRY_MILLIS = 50L;
    public static final int BOOK_CACHE_RETRY_COUNT = 10;
    public static final String BOOK_REVIEW_CACHE_PREFIX = "book:review:list:";
    public static final long BOOK_REVIEW_CACHE_MINUTES = 10L;
    public static final String BOOK_RATING_CACHE_PREFIX = "book:rating:cache:";
    public static final long BOOK_RATING_CACHE_MINUTES = 10L;
}
