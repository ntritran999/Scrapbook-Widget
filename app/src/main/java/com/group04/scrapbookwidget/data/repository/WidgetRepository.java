package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.Widget;
import com.group04.scrapbookwidget.data.service.WidgetService;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;

public class WidgetRepository implements IWidgetRepository {
    private WidgetService widgetService;
    @Inject
    WidgetRepository(WidgetService widgetService) {
        this.widgetService = widgetService;
    }
    @Override
    public List<Widget> getWidgets(String userId) throws IOException {
        try {
            Response<List<Widget>> response =  widgetService.getWidgets(userId).execute();
            if (response.isSuccessful()) {
                return response.body();
            }

        } catch (IOException e) {
            throw new IOException(e);
        }
        return null;
    }
}
