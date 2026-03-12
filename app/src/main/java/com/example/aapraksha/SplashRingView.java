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
    private final Paint ringPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashedRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Geometry ─────────────────────────────────────────────────────────────
    private float cx, cy;
    private float maxRadius;
    
    // ── Animation values ──────────────────────────────────────────────────────
    private float radarAngle = 0f;     // 0 -> 360 rotation
    private float glowIntensity = 0f;  // 0.0 -> 1.0

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

        // Standard Rings — Rose pink, low opacity
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(0xFFF48FB1); // Rose pink
        ringPaint.setStrokeWidth(1f * dp);
        ringPaint.setAlpha(55);

        // Dashed Ring — Slightly brighter, "shield circuit" feel
        dashedRingPaint.setStyle(Paint.Style.STROKE);
        dashedRingPaint.setColor(0xFFF48FB1);
        dashedRingPaint.setStrokeWidth(1.5f * dp);
        dashedRingPaint.setAlpha(100);
        dashedRingPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10f * dp, 8f * dp}, 0));

        // Radar Sweep — Crimson/Magenta gradient (set in onSizeChanged)
        sweepPaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        maxRadius = Math.min(w, h) * 0.55f;

        // Radar sweep gradient: Transparent → Crimson/Magenta
        SweepGradient sweepGradient = new SweepGradient(cx, cy,
                new int[]{0x00C2185B, 0x00C2185B, 0xBBC2185B}, // tail transparent → head crimson-magenta
                new float[]{0f, 0.75f, 1f});
        sweepPaint.setShader(sweepGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Draw Concentric Rings (Static)
        // Fixed proportions of maxRadius
        float[] ringRatios = {0.35f, 0.50f, 0.65f, 0.85f, 1.0f};
        
        for (int i = 0; i < ringRatios.length; i++) {
            float r = maxRadius * ringRatios[i];
            
            // Make the 3rd ring dashed for "tech" feel
            if (i == 2) { 
                canvas.drawCircle(cx, cy, r, dashedRingPaint);
            } else {
                canvas.drawCircle(cx, cy, r, ringPaint);
            }
        }

        // 2. Draw Radar Sweep
        canvas.save();
        canvas.rotate(radarAngle, cx, cy);
        canvas.drawCircle(cx, cy, maxRadius * 0.95f, sweepPaint); // Slightly smaller than largest ring
        canvas.restore();
        
        // 3. Center Glow (Lighthouse effect)
        if (glowIntensity > 0f) {
            drawCenterGlow(canvas);
        }
    }

    private void drawCenterGlow(Canvas canvas) {
        float r = maxRadius * 0.4f;
        // Layer 1: Broad rose/crimson halo
        RadialGradient halo = new RadialGradient(cx, cy, maxRadius * 1.1f,
                new int[]{
                    Color.argb((int)(35 * glowIntensity), 194, 24, 91),   // crimson-magenta
                    Color.argb((int)(15 * glowIntensity), 194, 24, 91),
                    Color.TRANSPARENT
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        glowPaint.setShader(halo);
        canvas.drawCircle(cx, cy, maxRadius * 1.1f, glowPaint);

        // Layer 2: Tight bright core (soft rose-white)
        RadialGradient core = new RadialGradient(cx, cy, r,
                new int[]{
                    Color.argb((int)(120 * glowIntensity), 255, 200, 220), // warm pink-white
                    Color.argb((int)(50  * glowIntensity), 194, 24, 91),   // mid crimson
                    Color.TRANSPARENT
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        glowPaint.setShader(core);
        canvas.drawCircle(cx, cy, r, glowPaint);
    }

    // ── Animation Setters ───────────────────────────────────────────────────
    public void setRadarAngle(float angle) {
        radarAngle = angle;
        invalidate();
    }
    
    public void setGlowIntensity(float intensity) {
        glowIntensity = intensity;
        invalidate();
    }
}
