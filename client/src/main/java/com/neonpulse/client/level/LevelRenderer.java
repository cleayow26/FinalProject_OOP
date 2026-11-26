
package com.neonpulse.client.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class LevelRenderer {

    private final ShapeRenderer shapes;

    private final Color cyan = new Color(0f, 0.85f, 1f, 1f);
    private final Color magenta = new Color(1f, 0.12f, 0.45f, 1f);
    private final Color purple = new Color(0.55f, 0.15f, 0.9f, 1f);
    private final Color groundColor = new Color(0.12f, 0.1f, 0.22f, 1f);

    public LevelRenderer(ShapeRenderer shapes) {
        this.shapes = shapes;
    }

    public void render(float cameraX, float screenWidth) {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Ground
        shapes.setColor(groundColor);
        shapes.rect(
                0,
                70,
                screenWidth,
                LevelData.GROUND_Y - 70
        );

        // Neon ground line
        shapes.setColor(cyan);
        shapes.rect(
                0,
                LevelData.GROUND_Y,
                screenWidth,
                4
        );

        // Platforms
        for (float[] platform : LevelData.PLATFORMS) {

            float x = platform[0] - cameraX;
            float y = platform[1];
            float width = platform[2];
            float height = platform[3];

            if (x + width < 0 || x > screenWidth) {
                continue;
            }

            shapes.setColor(purple);
            shapes.rect(x, y, width, height);

            shapes.setColor(cyan);
            shapes.rect(x, y + height - 3, width, 3);
        }

        // Spike obstacles
        for (float[] spike : LevelData.SPIKES) {

            float x = spike[0] - cameraX;
            float y = spike[1];
            float width = spike[2];
            float height = spike[3];

            if (x + width < 0 || x > screenWidth) {
                continue;
            }

            shapes.setColor(magenta);

            shapes.triangle(
                    x, y,
                    x + width / 2f, y + height,
                    x + width, y
            );
        }

        shapes.end();
    }
}
