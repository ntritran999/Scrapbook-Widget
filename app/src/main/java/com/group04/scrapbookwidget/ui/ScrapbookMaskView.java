package com.group04.scrapbookwidget.ui;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.Random;

public class ScrapbookMaskView extends AppCompatImageView {

    private Path maskPath = new Path();
    private final Random random = new Random();

    private Paint corePaint;
    private Paint dustPaint;
    private Paint shadowPaint;

    private static final int STEP = 10;
    private static final float MAX_NOISE = 35f;
    private static final float CORNER_JITTER = 40f;
    private boolean isMaskEnabled = false;

    public void setMaskEnabled(boolean enabled) {
        this.isMaskEnabled = enabled;
        invalidate(); // refresh view
    }

    public ScrapbookMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // disable hardware acceleration

        // SHADOW
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG); // smooth edges
        shadowPaint.setColor(Color.argb(90, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(60f);
        shadowPaint.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));

        // DUST
        dustPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dustPaint.setColor(Color.argb(220, 255, 255, 255));
        dustPaint.setStyle(Paint.Style.STROKE);
        dustPaint.setStrokeWidth(42f);
        dustPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));

        // CORE
        corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(Color.WHITE);
        corePaint.setStyle(Paint.Style.STROKE);
        corePaint.setStrokeWidth(22f);
        corePaint.setStrokeJoin(Paint.Join.ROUND);
        corePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        generateTornSquare(w, h);
    }

    private void generateTornSquare(int w, int h) {

        maskPath.reset();

        float size = Math.min(w, h);

        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        float right = left + size;
        float bottom = top + size;

        // make space for shadow
        float maxTearOutward = MAX_NOISE + (MAX_NOISE * 0.5f) + (CORNER_JITTER / 2f);
        float shadowSpread = shadowPaint.getStrokeWidth() / 2f + 25f;
        float shadowSpace = maxTearOutward + shadowSpread + 10f;
        left += shadowSpace;
        top += shadowSpace;
        right -= shadowSpace;
        bottom -= shadowSpace;

        // jitter 4 corner of photo
        PointF tl = jitter(left, top);
        PointF tr = jitter(right, top);
        PointF br = jitter(right, bottom);
        PointF bl = jitter(left, bottom);

        maskPath.moveTo(tl.x, tl.y);
        tearEdge(tl, tr, true);
        tearEdge(tr, br, false);
        tearEdge(br, bl, true);
        tearEdge(bl, tl, false);
        maskPath.close();
    }
    private PointF jitter(float x, float y) {
        return new PointF(
        x + (random.nextFloat() - 0.5f) * CORNER_JITTER,
        y + (random.nextFloat() - 0.5f) * CORNER_JITTER
        );
    }

    private void tearEdge(PointF start, PointF end, boolean horizontal) {

        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float dist = (float) Math.hypot(dx, dy); // pythagoras theorem

        int steps = Math.max(1, (int) (dist / STEP));
        float currentNoise = 0;

        for (int i = 1; i <= steps; i++) {

            float t = (float) i / steps;

            float baseX = start.x + dx * t;
            float baseY = start.y + dy * t;

            currentNoise += (random.nextFloat() - 0.5f) * 15f;
            currentNoise = Math.max(-MAX_NOISE, Math.min(MAX_NOISE, currentNoise));

            float organic = (float) Math.sin(t * Math.PI) * (MAX_NOISE * 0.5f);

            float finalX = baseX;
            float finalY = baseY;

            if (horizontal) finalY += currentNoise + organic;
            else finalX += currentNoise + organic;

            maskPath.lineTo(finalX, finalY);
        }

        maskPath.lineTo(end.x, end.y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isMaskEnabled) {
            super.onDraw(canvas);
            return;
        }

        canvas.drawPath(maskPath, shadowPaint);
        canvas.drawPath(maskPath, dustPaint);
        canvas.drawPath(maskPath, corePaint);

        canvas.save();
        canvas.clipPath(maskPath);

        super.onDraw(canvas);
        canvas.restore();
    }
}