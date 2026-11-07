package com.example.easyenchant;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** チャットで /enchui と打つとエンチャントUIが開く(クライアントコマンド)。 */
public class EasyEnchantClient implements ClientModInitializer {
    private static boolean openRequested = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommands.literal("enchui").executes(ctx -> {
                    openRequested = true; // チャット画面が閉じた後に開くため、tickで開く
                    return Command.SINGLE_SUCCESS;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openRequested) {
                openRequested = false;
                if (mc.player != null) {
                    mc.setScreen(new EnchantScreen());
                }
            }
        });
    }
}
