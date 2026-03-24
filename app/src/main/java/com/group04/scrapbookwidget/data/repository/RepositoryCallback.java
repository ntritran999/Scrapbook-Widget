package com.group04.scrapbookwidget.data.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception exception);
}
