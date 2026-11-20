
package com.neonpulse.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class NeonPulseGame extends ApplicationAdapter {

    private ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render() {

        Gdx.gl.glClearColor(0.035f, 0.035f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Ground
        shapeRenderer.setColor(0.10f, 0.10f, 0.20f, 1f);
        shapeRenderer.rect(0, 80, Gdx.graphics.getWidth(), 8);

        // Neon ground line
        shapeRenderer.setColor(0.0f, 0.9f, 1.0f, 1f);
        shapeRenderer.rect(0, 88, Gdx.graphics.getWidth(), 3);

        // Temporary player
        shapeRenderer.setColor(0.8f, 0.2f, 1.0f, 1f);
        shapeRenderer.rect(150, 88, 40, 60);

        // Platform
        shapeRenderer.setColor(0.1f, 0.8f, 1.0f, 1f);
        shapeRenderer.rect(350, 180, 180, 15);

        // Platform
        shapeRenderer.setColor(0.1f, 0.8f, 1.0f, 1f);
        shapeRenderer.rect(650, 260, 180, 15);

        // Obstacle
        shapeRenderer.setColor(1.0f, 0.2f, 0.4f, 1f);
        shapeRenderer.rect(550, 88, 30, 60);

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
