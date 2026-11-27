package com.neonpulse.client.objects.player;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neonpulse.client.level.LevelData;
import com.neonpulse.client.objects.GameObject;

public class Player extends GameObject {

    private static final float JUMP_HEIGHT = 130f; // pixels
    private static final float AIRTIME = 0.6f;     // seconds, on flat ground
    private static final float BUFFER_SECONDS = 0.1f;

    private static final float GRAVITY = 8f * JUMP_HEIGHT / (AIRTIME * AIRTIME);
    static final float JUMP_VELOCITY = 4f * JUMP_HEIGHT / AIRTIME;

    private PlayerState state;
    private float velocityY;
    private float bufferTimer;

    public Player() {
        super(LevelData.PLAYER_X, LevelData.GROUND_Y,
                LevelData.PLAYER_SIZE, LevelData.PLAYER_SIZE);
        reset();
    }

    public void reset() {
        x = LevelData.PLAYER_X;
        y = LevelData.GROUND_Y;
        velocityY = 0f;
        bufferTimer = 0f;
        changeState(new RunningState());
    }

    public void setX(float x) {
        this.x = x;
    }

    public void jump() {
        state.jump(this);
    }

    @Override
    public void update(float delta) {
        if (isDead()) {
            return;
        }
        state.update(this, delta);
        bufferTimer = Math.max(0f, bufferTimer - delta);

        if (LevelData.touchesSpike(x, y, width, height)) {
            die();
        }
    }

    @Override
    public void render(ShapeRenderer shapes, float cameraX) {
        float screenX = x - cameraX;

        shapes.setColor(0.45f, 0.1f, 0.8f, 1f); // glow
        shapes.rect(screenX - 5, y - 5, width + 10, height + 10);

        shapes.setColor(0.8f, 0.2f, 1f, 1f);    // cube
        shapes.rect(screenX, y, width, height);
    }

    void changeState(PlayerState newState) {
        state = newState;
        state.onEnter(this);
    }

    void bufferJump() {
        bufferTimer = BUFFER_SECONDS;
    }

    void land(float surfaceY) {
        y = surfaceY;
        velocityY = 0f;
        changeState(new RunningState());

        if (bufferTimer > 0f) {
            bufferTimer = 0f;
            jump();
        }
    }

    void applyGravity(float delta) {
        y += velocityY * delta - 0.5f * GRAVITY * delta * delta;
        velocityY -= GRAVITY * delta;
    }

    float surfaceBelow(float fromY) {
        return LevelData.surfaceBelow(x, width, fromY);
    }

    float getVelocityY() { return velocityY; }
    void setVelocityY(float velocityY) { this.velocityY = velocityY; }

    public void die() {
        if (!isDead()) {
            changeState(new DeadState());
        }
    }

    public boolean isDead() { return state instanceof DeadState; }
    public PlayerState getState() { return state; }
}
