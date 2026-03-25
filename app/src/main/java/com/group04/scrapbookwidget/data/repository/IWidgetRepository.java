package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.Widget;

import java.io.IOException;
import java.util.List;

public interface IWidgetRepository {
    List<Widget> getWidgets(String userId) throws IOException;
}
