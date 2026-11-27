package com.neonpulse.client.objects.player;

public class FallingState implements PlayerState {

    @Override
    public void update(Player player, float delta) {
        float previousY = player.getY();
        player.applyGravity(delta);

        float surfaceY = player.surfaceBelow(previousY);
        if (player.getY() <= surfaceY) {
            player.land(surfaceY);
        }
    }
}
