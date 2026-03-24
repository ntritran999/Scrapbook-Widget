package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.Template;

import java.util.List;

public interface ITemplateRepository {
    void getTemplateById(String templateId, RepositoryCallback<Template> callback);
    void getAllTemplates(RepositoryCallback<List<Template>> callback);
    void getTemplatesByCategory(String category, RepositoryCallback<List<Template>> callback);
    void createTemplate(Template template, RepositoryCallback<String> callback);
    void updateTemplate(String templateId, Template updatedTemplate, RepositoryCallback<Void> callback);
    void deleteTemplate(String templateId, RepositoryCallback<Void> callback);


    void getTemplateItems(String templateId, RepositoryCallback<List<ScrapbookItem>> callback);
    void getTemplateItem(String templateId, String itemId, RepositoryCallback<ScrapbookItem> callback);
    void addTemplateItem(String templateId, ScrapbookItem item, RepositoryCallback<String> callback);
    void updateTemplateItem(String templateId, String itemId, ScrapbookItem updatedItem, RepositoryCallback<Void> callback);
    void deleteTemplateItem(String templateId, String itemId, RepositoryCallback<Void> callback);
}
