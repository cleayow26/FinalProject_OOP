
package com.neonpulse.client.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import com.neonpulse.client.level.LevelData;
import com.neonpulse.client.level.LevelRenderer;

public class GameplayScreen extends ScreenAdapter {

    private final Game game;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    private LevelRenderer levelRenderer;

    private float elapsedTime = 0f;
    private float cameraX = 0f;

    private boolean paused = false;
    private boolean completed = false;

    private final float playerX = 150f;

    private float screenWidth;
    private float screenHeight;

    public GameplayScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        layout = new GlyphLayout();

        levelRenderer = new LevelRenderer(shapes);

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean keyDown(int keycode) {

                if (keycode == Input.Keys.ESCAPE && !completed) {
                    paused = !paused;
                    return true;
                }

                if (keycode == Input.Keys.R) {
                    restartLevel();
                    return true;
                }

                if (keycode == Input.Keys.M && (paused || completed)) {
                    returnToMenu();
                    return true;
                }

                return false;
            }

            @Override
            public boolean touchDown(
                    int screenX,
                    int screenY,
                    int pointer,
                    int button
            ) {

                float x = screenX;
                float y = Gdx.graphics.getHeight() - screenY;

                float width = Gdx.graphics.getWidth();
                float height = Gdx.graphics.getHeight();

                // Level Complete buttons
                if (completed) {

                    if (insideButton(
                            x, y,
                            width / 2f - 130,
                            height / 2f - 65,
                            260, 55)) {

                        restartLevel();
                        return true;
                    }

                    if (insideButton(
                            x, y,
                            width / 2f - 130,
                            height / 2f - 135,
                            260, 55)) {

                        returnToMenu();
                        return true;
                    }

                    return true;
                }

                // Pause menu buttons
                if (paused) {

                    if (insideButton(
                            x, y,
                            width / 2f - 130,
                            height / 2f + 5,
                            260, 50)) {

                        paused = false;
                        return true;
                    }

                    if (insideButton(
                            x, y,
                            width / 2f - 130,
                            height / 2f - 60,
                            260, 50)) {

                        restartLevel();
                        return true;
                    }

                    if (insideButton(
                            x, y,
                            width / 2f - 130,
                            height / 2f - 125,
                            260, 50)) {

                        returnToMenu();
                        return true;
                    }

                    return true;
                }

                // Pause button
                if (insideButton(
                        x, y,
                        width - 85,
                        height - 65,
                        55, 45)) {

                    paused = true;
                    return true;
                }

                return false;
            }
        });
    }

    private boolean insideButton(
            float x,
            float y,
            float bx,
            float by,
            float width,
            float height
    ) {

        return x >= bx
                && x <= bx + width
                && y >= by
                && y <= by + height;
    }

    @Override
    public void render(float delta) {

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        if (!paused && !completed) {

            elapsedTime += delta;

            float maxCameraX =
                    LevelData.LEVEL_LENGTH - screenWidth;

            cameraX = Math.min(
                    elapsedTime * 150f,
                    maxCameraX
            );

            if (cameraX >= maxCameraX) {
                completed = true;
            }
        }

        Gdx.gl.glClearColor(
                0.025f,
                0.025f,
                0.075f,
                1f
        );

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground();

        levelRenderer.render(cameraX, screenWidth);

        drawPlayer();

        drawHUD();

        if (paused) {
            drawPauseMenu();
        }

        if (completed) {
            drawLevelComplete();
        }
    }

    private void drawBackground() {

        shapes.begin(ShapeRenderer.ShapeType.Line);

        shapes.setColor(
                0.13f,
                0.09f,
                0.25f,
                0.4f
        );

        for (int x = 0; x < screenWidth; x += 60) {
            shapes.line(x, 0, x, screenHeight);
        }

        for (int y = 0; y < screenHeight; y += 60) {
            shapes.line(0, y, screenWidth, y);
        }

        shapes.end();
    }

    private void drawPlayer() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Player glow
        shapes.setColor(
                0.45f,
                0.1f,
                0.8f,
                1f
        );

        shapes.rect(
                playerX - 5,
                LevelData.GROUND_Y - 5,
                LevelData.PLAYER_SIZE + 10,
                LevelData.PLAYER_SIZE + 10
        );

        // Player cube
        shapes.setColor(
                0.8f,
                0.2f,
                1f,
                1f
        );

        shapes.rect(
                playerX,
                LevelData.GROUND_Y,
                LevelData.PLAYER_SIZE,
                LevelData.PLAYER_SIZE
        );

        shapes.end();
    }

    private void drawHUD() {

        float progress = Math.min(
                cameraX / (LevelData.LEVEL_LENGTH - screenWidth),
                1f
        );

        batch.begin();

        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);

        font.draw(
                batch,
                "NEON PULSE",
                30,
                screenHeight - 30
        );

        font.getData().setScale(1.2f);

        String percentage =
                String.format("%d%%", (int) (progress * 100));

        layout.setText(font, percentage);

        font.draw(
                batch,
                percentage,
                screenWidth - 150,
                screenHeight - 30
        );

        font.getData().setScale(0.9f);
        font.setColor(Color.CYAN);

        font.draw(
                batch,
                "ESC : PAUSE    R : RESTART",
                30,
                screenHeight - 65
        );

        batch.end();

        // Progress bar
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.18f, 0.12f, 0.3f, 1f);

        shapes.rect(
                30,
                screenHeight - 90,
                screenWidth - 60,
                8
        );

        shapes.setColor(0f, 0.85f, 1f, 1f);

        shapes.rect(
                30,
                screenHeight - 90,
                (screenWidth - 60) * progress,
                8
        );

        // Pause button
        shapes.setColor(0.35f, 0.12f, 0.55f, 1f);

        shapes.rect(
                screenWidth - 85,
                screenHeight - 65,
                55,
                45
        );

        shapes.end();

        batch.begin();

        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);

        font.draw(
                batch,
                "II",
                screenWidth - 68,
                screenHeight - 35
        );

        batch.end();
    }

    private void drawPauseMenu() {

        drawOverlay();

        drawPanel(
                screenWidth / 2f - 180,
                screenHeight / 2f - 160,
                360,
                320
        );

        drawButton(
                "RESUME",
                screenWidth / 2f - 130,
                screenHeight / 2f + 5,
                260,
                50,
                Color.CYAN
        );

        drawButton(
                "RESTART",
                screenWidth / 2f - 130,
                screenHeight / 2f - 60,
                260,
                50,
                Color.MAGENTA
        );

        drawButton(
                "MAIN MENU",
                screenWidth / 2f - 130,
                screenHeight / 2f - 125,
                260,
                50,
                Color.PURPLE
        );

        drawTitle(
                "PAUSED",
                screenHeight / 2f + 115
        );
    }

    private void drawLevelComplete() {

        drawOverlay();

        drawPanel(
                screenWidth / 2f - 200,
                screenHeight / 2f - 160,
                400,
                320
        );

        drawTitle(
                "LEVEL COMPLETE!",
                screenHeight / 2f + 100
        );

        drawText(
                "100% COMPLETE",
                screenHeight / 2f + 45,
                Color.CYAN,
                1.3f
        );

        drawButton(
                "RETRY",
                screenWidth / 2f - 130,
                screenHeight / 2f - 65,
                260,
                55,
                Color.CYAN
        );

        drawButton(
                "MAIN MENU",
                screenWidth / 2f - 130,
                screenHeight / 2f - 135,
                260,
                55,
                Color.MAGENTA
        );
    }

    private void drawOverlay() {

        Gdx.gl.glEnable(GL20.GL_BLEND);

        Gdx.gl.glBlendFunc(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0f, 0f, 0f, 0.8f);

        shapes.rect(
                0,
                0,
                screenWidth,
                screenHeight
        );

        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawPanel(
            float x,
            float y,
            float width,
            float height
    ) {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.09f, 0.055f, 0.19f, 1f);

        shapes.rect(x, y, width, height);

        shapes.setColor(Color.CYAN);

        shapes.rect(
                x,
                y + height - 4,
                width,
                4
        );

        shapes.end();
    }

    private void drawButton(
            String text,
            float x,
            float y,
            float width,
            float height,
            Color accent
    ) {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.20f, 0.09f, 0.34f, 1f);

        shapes.rect(x, y, width, height);

        shapes.setColor(accent);

        shapes.rect(
                x,
                y + height - 3,
                width,
                3
        );

        shapes.end();

        batch.begin();

        font.getData().setScale(1.3f);
        font.setColor(Color.WHITE);

        layout.setText(font, text);

        font.draw(
                batch,
                text,
                x + (width - layout.width) / 2f,
                y + (height + layout.height) / 2f
        );

        batch.end();
    }

    private void drawTitle(String text, float y) {

        drawText(text, y, Color.WHITE, 2.3f);
    }

    private void drawText(
            String text,
            float y,
            Color color,
            float scale
    ) {

        batch.begin();

        font.getData().setScale(scale);
        font.setColor(color);

        layout.setText(font, text);

        font.draw(
                batch,
                text,
                (screenWidth - layout.width) / 2f,
                y
        );

        batch.end();
    }

    private void restartLevel() {

        elapsedTime = 0f;
        cameraX = 0f;

        paused = false;
        completed = false;
    }

    private void returnToMenu() {

        game.setScreen(new MainMenuScreen(game));
        dispose();
    }

    @Override
    public void dispose() {

        if (shapes != null) shapes.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
