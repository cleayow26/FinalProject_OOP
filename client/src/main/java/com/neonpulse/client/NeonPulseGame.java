
package com.neonpulse.client;

import com.badlogic.gdx.Game;
import com.neonpulse.client.screens.MainMenuScreen;

public class NeonPulseGame extends Game {

    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
