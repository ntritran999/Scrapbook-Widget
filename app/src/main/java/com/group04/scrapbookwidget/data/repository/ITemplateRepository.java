package com.group04.scrapbookwidget.data.repository;

import com.google.android.gms.tasks.Task;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.Template;

import java.util.List;

public interface ITemplateRepository {
    Task<Template> getTemplateById(String templateId);
    Task<List<Template>> getAllTemplates();
    Task<List<Template>> getTemplatesByCategory(String category);
    Task<String> createTemplate(Template template);
    Task<Void> updateTemplate(String templateId, Template updatedTemplate);
    Task<Void> deleteTemplate(String templateId);


    Task<List<ScrapbookItem>> getTemplateItems(String templateId);
    Task<ScrapbookItem> getTemplateItem(String templateId, String itemId);
    Task<String> addTemplateItem(String templateId, ScrapbookItem item);
    Task<Void> updateTemplateItem(String templateId, String itemId, ScrapbookItem updatedItem);
    Task<Void> deleteTemplateItem(String templateId, String itemId);
}
