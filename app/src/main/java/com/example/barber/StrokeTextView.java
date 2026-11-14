package com.example.barber;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class StrokeTextView extends AppCompatTextView {
    private float strokeWidth = 12f;
    private int strokeColor = 0xFFFFFFFF; // Color blanco para el borde

    public StrokeTextView(Context context) {
        super(context);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        invalidate();
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int originalColor = getCurrentTextColor();

        // Dibujar el contorno / borde blanco
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(strokeWidth);
        setTextColor(strokeColor);
        super.onDraw(canvas);

        // Dibujar el relleno negro del texto
        getPaint().setStyle(Paint.Style.FILL);
        setTextColor(originalColor);
        super.onDraw(canvas);
    }
}
