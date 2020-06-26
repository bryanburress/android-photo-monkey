package com.chesapeaketechnology.photomonkey.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * An overlay view used to draw the focus rectangle on the view finder.
 */
class FocusRectangleView extends View
{
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    DashPathEffect pathEffect;
    private Color color;
    private float left;
    private float top;
    private float right;
    private float bottom;
    private float strokeWidth;

    public FocusRectangleView(Context context)
    {
        super(context);
    }

    public FocusRectangleView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public FocusRectangleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public FocusRectangleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Get the stroke width for the rectangle corner brackets.
     */
    public float getStrokeWidth()
    {
        return strokeWidth;
    }

    /**
     * Set the stroke width for the rectangle corner brackets.
     *
     * @param strokeWidth a float representing the width.
     */
    public void setStrokeWidth(float strokeWidth)
    {
        this.strokeWidth = strokeWidth;
    }

    /**
     * Get the color used for the focus rectangle
     *
     * @return {@link Color} object representing the color to use for the focus rectangle.
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Set the color used for the focus rectangle.
     *
     * @param color a {@link Color} object representing the color to use for the focus rectangle.
     */
    public void setColor(Color color)
    {
        this.color = color;
    }

    /**
     * Uses {@link DashPathEffect} to create corner brackets for the rectangle.
     *
     * @param size the {@link Size} of the rectangle.
     */
    private void createBracketEffect(Size size)
    {
        double bracket_size_pct = 0.16;
        int horizontal_size = (int) (size.getWidth() * bracket_size_pct);
        int horizontal_offset = size.getWidth() - (horizontal_size * 2);
        int vertical_size = (int) (size.getHeight() * bracket_size_pct);
        int vertical_offset = size.getHeight() - (vertical_size * 2);

        pathEffect = new DashPathEffect(
                new float[]{
                        horizontal_size, horizontal_offset, horizontal_size, 0,
                        vertical_size, vertical_offset, vertical_size, 0,
                        horizontal_size, horizontal_offset, horizontal_size, 0,
                        vertical_size, vertical_size, vertical_offset, 0},
                0);
    }

    /**
     * Set the location for the focus rectangle.
     *
     * @param center A {@link Point} object representing the center point of the rectangle.
     * @param size   A {@link Size} object representing the size of the rectangle.
     */
    public void setFocusLocation(Point center, Size size)
    {
        left = center.x - (float) size.getWidth() / 2;
        top = center.y - (float) size.getHeight() / 2;
        right = center.x + (float) size.getWidth() / 2;
        bottom = center.y + (float) size.getHeight() / 2;
        createBracketEffect(size);
        invalidate();
    }

    /**
     * Hide the focus rectangle and reset underlying values to 0.
     */
    public void clear()
    {
        left = 0;
        top = 0;
        right = 0;
        bottom = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (left > 0)
        {
            paint.setColor(color.toArgb());
            paint.setPathEffect(pathEffect);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            canvas.drawRect(left, top, right, bottom, paint);
        }
        super.onDraw(canvas);
    }
}
