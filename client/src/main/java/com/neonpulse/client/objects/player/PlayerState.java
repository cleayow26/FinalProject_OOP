package com.neonpulse.client.objects.player;

public interface PlayerState {

    default void onEnter(Player player) {}

    default void jump(Player player) {
        player.bufferJump();
    }

    void update(Player player, float delta);
}
