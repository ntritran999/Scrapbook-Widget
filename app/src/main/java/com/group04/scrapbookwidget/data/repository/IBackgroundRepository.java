package com.group04.scrapbookwidget.data.repository;

import java.util.List;

public interface IBackgroundRepository {
    void getBackground(RepositoryCallback<List<String>> callback);
}
