package com.neonpulse.client.objects;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class GameObject {

    protected float x;
    protected float y;
    protected final float width;
    protected final float height;

    protected GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(float delta);

    public abstract void render(ShapeRenderer shapes, float cameraX);

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
}
