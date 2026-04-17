package com.group04.scrapbookwidget.ui.camera;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ScrapbookMaskView extends FrameLayout {

    private Path stampOuterPath = new Path();
    private Path innerImagePath = new Path();

    private Paint corePaint;
    private Paint shadowPaint;

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
        setLayerType(LAYER_TYPE_SOFTWARE, null); // disable hardware acceleration to support BlurMaskFilter

        // SHADOW
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));

        // CORE
        corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(Color.WHITE);
        corePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        generateStampPath(w, h);
    }

    private void generateStampPath(int w, int h) {
        stampOuterPath.reset();
        innerImagePath.reset();

        float size = Math.min(w, h);
        float shadowSpread = 30f;

        float left = (w - size) / 2f + shadowSpread;
        float top = (h - size) / 2f + shadowSpread;
        float right = w - (w - size) / 2f - shadowSpread;
        float bottom = h - (h - size) / 2f - shadowSpread;

        float holeRadius = 12f;
        float holeSpacing = 42f;

        float edgeLength = right - left;
        int numHoles = Math.max(1, Math.round(edgeLength / holeSpacing));
        float actualSpacing = edgeLength / numHoles;

        stampOuterPath.moveTo(left, top);

        for (int i = 1; i <= numHoles; i++) {
            float cx = left + i * actualSpacing - actualSpacing / 2f;
            stampOuterPath.lineTo(cx - holeRadius, top);
            stampOuterPath.arcTo(new RectF(cx - holeRadius, top - holeRadius, cx + holeRadius, top + holeRadius), 180, -180);
        }
        stampOuterPath.lineTo(right, top);

        for (int i = 1; i <= numHoles; i++) {
            float cy = top + i * actualSpacing - actualSpacing / 2f;
            stampOuterPath.lineTo(right, cy - holeRadius);
            stampOuterPath.arcTo(new RectF(right - holeRadius, cy - holeRadius, right + holeRadius, cy + holeRadius), 270, -180);
        }
        stampOuterPath.lineTo(right, bottom);

        for (int i = 1; i <= numHoles; i++) {
            float cx = right - (i * actualSpacing - actualSpacing / 2f);
            stampOuterPath.lineTo(cx + holeRadius, bottom);
            stampOuterPath.arcTo(new RectF(cx - holeRadius, bottom - holeRadius, cx + holeRadius, bottom + holeRadius), 0, -180);
        }
        stampOuterPath.lineTo(left, bottom);

        for (int i = 1; i <= numHoles; i++) {
            float cy = bottom - (i * actualSpacing - actualSpacing / 2f);
            stampOuterPath.lineTo(left, cy + holeRadius);
            stampOuterPath.arcTo(new RectF(left - holeRadius, cy - holeRadius, left + holeRadius, cy + holeRadius), 90, -180);
        }
        stampOuterPath.lineTo(left, top);
        stampOuterPath.close();

        float borderWidth = holeRadius + 26f;
        innerImagePath.addRect(left + borderWidth, top + borderWidth, right - borderWidth, bottom - borderWidth, Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!isMaskEnabled) {
            super.dispatchDraw(canvas);
            return;
        }

        canvas.drawPath(stampOuterPath, shadowPaint);

        canvas.drawPath(stampOuterPath, corePaint);

        canvas.save();
        canvas.clipPath(innerImagePath);

        super.dispatchDraw(canvas);

        canvas.restore();
    }
}