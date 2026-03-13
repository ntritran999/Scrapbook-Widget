package com.group04.scrapbookwidget.ui;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

public class ScrapbookDrawView extends AppCompatImageView {
    private Paint currentPaint;
    private Path currentPath;
    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 4f;
    private boolean isDrawingEnabled = false;

    private List<DrawAction> actions = new ArrayList<>();

    private static class DrawAction {
        Path path;
        Paint paint;

        DrawAction(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

    public void setDrawingEnabled(boolean enabled) {
        this.isDrawingEnabled = enabled;
    }

    public ScrapbookDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // disable hardware acceleration

        currentPath = new Path();
        currentPaint = new Paint();

        currentPaint.setAntiAlias(true);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeWidth(12f);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);

        changeColor(Color.parseColor("#FFCC00"));
        setBrushType(BrushType.PENCIL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(touchX, touchY);
                lastX = touchX;
                lastY = touchY;
                actions.add(new DrawAction(currentPath, new Paint(currentPaint)));
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(touchX - lastX);
                float dy = Math.abs(touchY - lastY);

                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    currentPath.quadTo(lastX, lastY, (touchX + lastX) / 2, (touchY + lastY) / 2);
                    lastX = touchX;
                    lastY = touchY;
                }
                break;

            case MotionEvent.ACTION_UP:
                currentPath.lineTo(touchX, touchY);
                break;

            default:
                return false;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (DrawAction action : actions) {
            canvas.drawPath(action.path, action.paint);
        }
    }

    public void clearDrawing() {
        actions.clear();
        currentPath = new Path();
        invalidate();
    }

    public void changeColor(int color) {
        currentPaint.setColor(color);
    }

    public enum BrushType {
        PENCIL,
        BALLPOINT,
        HIGHLIGHTER,
        SPRAY,
        ERASER
    }

    public void setBrushType(BrushType type) {
        currentPaint.reset();

        currentPaint.setAntiAlias(true);
        currentPaint.setDither(true);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);

        switch (type) {
            case BALLPOINT:
                currentPaint.setColor(Color.rgb(0, 0, 180));
                currentPaint.setStrokeWidth(4f);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);
                currentPaint.setAntiAlias(true);
                currentPaint.setMaskFilter(null);
                currentPaint.setPathEffect(null);
                currentPaint.setAlpha(255);

                break;
            case HIGHLIGHTER:
                currentPaint.setColor(Color.YELLOW);
                currentPaint.setStrokeWidth(40f);
                currentPaint.setStrokeCap(Paint.Cap.SQUARE);
                currentPaint.setAlpha(90);
                currentPaint.setXfermode(new PorterDuffXfermode(
                        PorterDuff.Mode.MULTIPLY
                ));
                currentPaint.setMaskFilter(null);
                currentPaint.setPathEffect(null);

                break;
            case SPRAY:
                currentPaint.setColor(Color.BLACK);
                currentPaint.setStrokeWidth(30f);
                currentPaint.setMaskFilter(new BlurMaskFilter(
                        15f,
                        BlurMaskFilter.Blur.NORMAL
                ));

                currentPaint.setAlpha(130);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);

                break;
            case ERASER:
                currentPaint.setColor(Color.TRANSPARENT);
                currentPaint.setStrokeWidth(60f);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);
                currentPaint.setXfermode(new PorterDuffXfermode(
                        PorterDuff.Mode.CLEAR
                ));
                currentPaint.setMaskFilter(null);
                currentPaint.setPathEffect(null);

                break;
            default: // PENCIL
                currentPaint.setColor(Color.parseColor("#4B4B4B"));
                currentPaint.setStrokeWidth(4f);
                PathEffect roughEdge = new DiscretePathEffect(3f, 1.5f);
                PathEffect smoothCorner = new CornerPathEffect(1f);
                currentPaint.setPathEffect(new ComposePathEffect(roughEdge, smoothCorner));
                currentPaint.setMaskFilter(new BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL));
                currentPaint.setAlpha(120);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);
                currentPaint.setStrokeJoin(Paint.Join.ROUND);
                break;
        }
    }
}
