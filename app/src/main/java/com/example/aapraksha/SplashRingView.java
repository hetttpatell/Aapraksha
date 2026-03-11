package com.example.aapraksha;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

/**
 * SplashRingView - Custom View for AapRaksha Splash Screen
 *
 * Renders:
 *   1. Multi-layer lighthouse radial glow from center
 *   2. Two thin concentric rings (soft lavender/indigo)
 *   3. 90° (1/4) thick rotating arc on the inner ring (clockwise)
 *   4. 90° (1/4) thick rotating arc on the outer ring (counter-clockwise)
 *
 * Animated properties (called by SplashActivity):
 *   setArcAngle(float)       — drives arc rotation (0–720°)
 *   setGlowIntensity(float)  — drives glow opacity (0.0–1.0)
 */
public class SplashRingView extends View {

    // ── Paints ──────────────────────────────────────────────────────────────
    private final Paint thinRingInner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thinRingOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thickArcInner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thickArcOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloRingInner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloRingOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Geometry ─────────────────────────────────────────────────────────────
    private final RectF innerRect = new RectF();
    private final RectF outerRect = new RectF();
    private float cx, cy;
    private float innerRadius, outerRadius;

    // ── Animation values ──────────────────────────────────────────────────────
    private float arcAngle     = 0f;   // drives rotation
    private float glowIntensity = 0f;  // 0.0 → 1.0

    // ── Constructor ───────────────────────────────────────────────────────────
    public SplashRingView(Context context) {
        super(context); init();
    }
    public SplashRingView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public SplashRingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        final float dp = getResources().getDisplayMetrics().density;

        // inner thin ring — soft lavender, 30% opaque, 1.5dp
        thinRingInner.setStyle(Paint.Style.STROKE);
        thinRingInner.setColor(0x4DBEE1E6);
        thinRingInner.setStrokeWidth(1.5f * dp);

        // outer thin ring — lighter lavender, 20% opaque, 1dp
        thinRingOuter.setStyle(Paint.Style.STROKE);
        thinRingOuter.setColor(0x338BE0E6);
        thinRingOuter.setStrokeWidth(1f * dp);

        // inner halo (extra subtle glow ring at 0.5dp)
        haloRingInner.setStyle(Paint.Style.STROKE);
        haloRingInner.setColor(0x1A4361EE);
        haloRingInner.setStrokeWidth(0.5f * dp);

        // outer halo
        haloRingOuter.setStyle(Paint.Style.STROKE);
        haloRingOuter.setColor(0x124361EE);
        haloRingOuter.setStrokeWidth(0.5f * dp);

        // thick arc inner — 5dp, gradient set per-draw
        thickArcInner.setStyle(Paint.Style.STROKE);
        thickArcInner.setStrokeWidth(5f * dp);
        thickArcInner.setStrokeCap(Paint.Cap.ROUND);

        // thick arc outer — 3dp, gradient set per-draw
        thickArcOuter.setStyle(Paint.Style.STROKE);
        thickArcOuter.setStrokeWidth(3f * dp);
        thickArcOuter.setStrokeCap(Paint.Cap.ROUND);

        setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for RadialGradient
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float dp = getResources().getDisplayMetrics().density;
        cx = w / 2f;
        cy = h / 2f;
        innerRadius = 115f * dp;
        outerRadius = 152f * dp;

        innerRect.set(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius);
        outerRect.set(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Step 1 — lighthouse glow
        if (glowIntensity > 0f) {
            drawLighthouseGlow(canvas);
        }

        // Step 2 — thin static rings
        canvas.drawOval(innerRect, thinRingInner);
        canvas.drawOval(outerRect, thinRingOuter);

        // Step 3 — clockwise rotating 90° arc on inner ring
        SweepGradient sgInner = new SweepGradient(cx, cy,
                new int[]{0x004361EE, 0xCC4361EE, 0xFFBEE1E6, 0xCC4361EE, 0x004361EE},
                new float[]{0f, 0.05f, 0.14f, 0.22f, 0.25f});
        thickArcInner.setShader(sgInner);
        canvas.save();
        canvas.rotate(arcAngle, cx, cy);
        canvas.drawArc(innerRect, -90f, 90f, false, thickArcInner);
        canvas.restore();

        // Step 4 — counter-clockwise 90° arc on outer ring
        SweepGradient sgOuter = new SweepGradient(cx, cy,
                new int[]{0x00BEE1E6, 0x99BEE1E6, 0xFFBEE1E6, 0x99BEE1E6, 0x00BEE1E6},
                new float[]{0f, 0.04f, 0.12f, 0.20f, 0.25f});
        thickArcOuter.setShader(sgOuter);
        canvas.save();
        canvas.rotate(-arcAngle * 0.65f + 45f, cx, cy);
        canvas.drawArc(outerRect, -90f, 90f, false, thickArcOuter);
        canvas.restore();
    }

    /** Lighthouse-style multi-layer radial glow emanating from the logo center. */
    private void drawLighthouseGlow(Canvas canvas) {
        float intensity = glowIntensity;

        // Layer 1: broad soft indigo halo
        float r1 = outerRadius * 1.8f;
        RadialGradient g1 = new RadialGradient(cx, cy, r1,
                new int[]{
                        Color.argb((int)(55 * intensity), 67, 97, 238),
                        Color.argb((int)(25 * intensity), 67, 97, 238),
                        Color.argb((int)(8  * intensity), 67, 97, 238),
                        Color.argb(0, 15, 23, 42)
                },
                new float[]{0f, 0.35f, 0.65f, 1f},
                Shader.TileMode.CLAMP);
        glowPaint.setShader(g1);
        canvas.drawCircle(cx, cy, r1, glowPaint);

        // Layer 2: tight bright core glow (white-lavender)
        float r2 = innerRadius * 0.85f;
        RadialGradient g2 = new RadialGradient(cx, cy, r2,
                new int[]{
                        Color.argb((int)(80 * intensity), 255, 255, 255),
                        Color.argb((int)(45 * intensity), 190, 225, 230),
                        Color.argb(0, 67, 97, 238)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        glowPaint.setShader(g2);
        canvas.drawCircle(cx, cy, r2, glowPaint);
    }

    // ── Animated properties ───────────────────────────────────────────────────
    public void setArcAngle(float angle) {
        arcAngle = angle;
        invalidate();
    }
    public float getArcAngle() { return arcAngle; }

    public void setGlowIntensity(float intensity) {
        glowIntensity = intensity;
        invalidate();
    }
    public float getGlowIntensity() { return glowIntensity; }
}
