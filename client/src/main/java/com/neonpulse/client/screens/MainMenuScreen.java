
package com.neonpulse.client.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class MainMenuScreen extends ScreenAdapter {

    private final Game game;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    private float time = 0f;

    private final float buttonWidth = 260;
    private final float buttonHeight = 60;

    private float startX, startY;
    private float exitX, exitY;

    private final Color background =
            new Color(0.025f, 0.025f, 0.075f, 1f);

    private final Color cyan =
            new Color(0f, 0.85f, 1f, 1f);

    private final Color purple =
            new Color(0.65f, 0.15f, 1f, 1f);

    private final Color magenta =
            new Color(1f, 0.15f, 0.5f, 1f);

    public MainMenuScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        layout = new GlyphLayout();

        updateButtonPositions();

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchDown(
                    int screenX,
                    int screenY,
                    int pointer,
                    int button
            ) {

                float x = screenX;
                float y = Gdx.graphics.getHeight() - screenY;

                if (insideButton(x, y, startX, startY)) {

                    game.setScreen(new GameplayScreen(game));
                    dispose();

                    return true;
                }

                if (insideButton(x, y, exitX, exitY)) {

                    Gdx.app.exit();
                    return true;
                }

                return false;
            }
        });
    }

    private void updateButtonPositions() {

        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        startX = centerX - buttonWidth / 2f;
        startY = centerY - 55;

        exitX = centerX - buttonWidth / 2f;
        exitY = centerY - 145;
    }

    private boolean insideButton(
            float x,
            float y,
            float bx,
            float by
    ) {

        return x >= bx
                && x <= bx + buttonWidth
                && y >= by
                && y <= by + buttonHeight;
    }

    @Override
    public void render(float delta) {

        time += delta;

        updateButtonPositions();

        Gdx.gl.glClearColor(
                background.r,
                background.g,
                background.b,
                background.a
        );

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        drawBackground(width, height);
        drawButtons();
        drawText(width, height);
    }

    private void drawBackground(float width, float height) {

        shapes.begin(ShapeRenderer.ShapeType.Line);

        // Neon grid
        shapes.setColor(0.15f, 0.1f, 0.3f, 0.35f);

        for (int x = 0; x < width; x += 60) {
            shapes.line(x, 0, x, height);
        }

        for (int y = 0; y < height; y += 60) {
            shapes.line(0, y, width, y);
        }

        // Animated neon line
        float pulse = 0.5f + 0.5f *
                (float) Math.sin(time * 2f);

        shapes.setColor(
                cyan.r,
                cyan.g,
                cyan.b,
                0.4f + pulse * 0.6f
        );

        shapes.rectLine(0, 90, width, 90, 3);

        shapes.end();
    }

    private void drawButtons() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Start button
        shapes.setColor(0.25f, 0.08f, 0.45f, 1f);

        shapes.rect(
                startX,
                startY,
                buttonWidth,
                buttonHeight
        );

        shapes.setColor(cyan);

        shapes.rect(
                startX,
                startY + buttonHeight - 3,
                buttonWidth,
                3
        );

        // Exit button
        shapes.setColor(0.15f, 0.08f, 0.25f, 1f);

        shapes.rect(
                exitX,
                exitY,
                buttonWidth,
                buttonHeight
        );

        shapes.setColor(magenta);

        shapes.rect(
                exitX,
                exitY + buttonHeight - 3,
                buttonWidth,
                3
        );

        shapes.end();
    }

    private void drawText(float width, float height) {

        batch.begin();

        // Main title
        font.getData().setScale(4f);
        font.setColor(purple);

        String title = "NEON PULSE";

        layout.setText(font, title);

        font.draw(
                batch,
                title,
                (width - layout.width) / 2f,
                height * 0.72f
        );

        // Subtitle
        font.getData().setScale(1.2f);
        font.setColor(cyan);

        String subtitle = "JUMP. DODGE. SYNC.";

        layout.setText(font, subtitle);

        font.draw(
                batch,
                subtitle,
                (width - layout.width) / 2f,
                height * 0.62f
        );

        // Start button
        font.getData().setScale(1.6f);
        font.setColor(Color.WHITE);

        String startText = "START GAME";

        layout.setText(font, startText);

        font.draw(
                batch,
                startText,
                startX + (buttonWidth - layout.width) / 2f,
                startY + 38
        );

        // Exit button
        String exitText = "EXIT";

        layout.setText(font, exitText);

        font.draw(
                batch,
                exitText,
                exitX + (buttonWidth - layout.width) / 2f,
                exitY + 38
        );

        // Footer
        font.getData().setScale(0.9f);
        font.setColor(Color.LIGHT_GRAY);

        String footer = "NEON PULSE - 2D RHYTHM PLATFORMER";

        layout.setText(font, footer);

        font.draw(
                batch,
                footer,
                (width - layout.width) / 2f,
                35
        );

        batch.end();
    }

    @Override
    public void dispose() {

        if (shapes != null) shapes.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
