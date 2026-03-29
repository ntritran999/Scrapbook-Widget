package com.group04.scrapbookwidget.data.model;

public class Layout {
    public float x, y, width, height, rotation, scale, zIndex;

    public Layout() {}

    public Layout(float x, float y, float w, float h, float r, float s, float zIndex) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.rotation = r;
        this.scale = s;
        this.zIndex = zIndex;
    }
}
