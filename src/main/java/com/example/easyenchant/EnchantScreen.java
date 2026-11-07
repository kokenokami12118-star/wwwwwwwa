package com.example.easyenchant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * エンチャント選択UI。ボタンとテキスト欄だけで構成(描画APIに依存しない)。
 * 適用は /ench コマンドを送信して行う(OP権限が必要)。
 */
public class EnchantScreen extends Screen {
    private static final int PER_PAGE = 8;

    private final List<Holder.Reference<Enchantment>> all = new ArrayList<>();
    private int page = 0;
    private int selected = -1;
    private EditBox levelBox;
    private Button applyButton;
    private Button[] listButtons;

    public EnchantScreen() {
        super(Component.literal("エンチャント"));
    }

    @Override
    protected void init() {
        all.clear();
        var level = Minecraft.getInstance().level;
        if (level != null) {
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .listElements().forEach(all::add);
            all.sort((a, b) -> a.getRegisteredName().compareTo(b.getRegisteredName()));
        }

        int cx = this.width / 2;
        int top = this.height / 2 - 100;

        // エンチャント一覧(8個ずつ)
        listButtons = new Button[PER_PAGE];
        for (int i = 0; i < PER_PAGE; i++) {
            final int slot = i;
            listButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                selected = page * PER_PAGE + slot;
                refresh();
            }).bounds(cx - 100, top + i * 22, 200, 20).build());
        }

        // ページ送り
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) page--;
            refresh();
        }).bounds(cx - 100, top + 182, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if ((page + 1) * PER_PAGE < all.size()) page++;
            refresh();
        }).bounds(cx + 70, top + 182, 30, 20).build());

        // レベル入力
        levelBox = new EditBox(this.font, cx - 100, top + 208, 60, 20, Component.literal("レベル"));
        levelBox.setMaxLength(3);
        levelBox.setFilter(s -> s.matches("\\d{0,3}"));
        levelBox.setValue("255");
        addRenderableWidget(levelBox);

        addRenderableWidget(Button.builder(Component.literal("-10"), b -> addLevel(-10))
                .bounds(cx - 36, top + 208, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> addLevel(10))
                .bounds(cx - 4, top + 208, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("最大"), b -> levelBox.setValue("255"))
                .bounds(cx + 28, top + 208, 40, 20).build());

        // 適用 / 全部 / 消す
        applyButton = addRenderableWidget(Button.builder(Component.literal("付与"), b -> send(false))
                .bounds(cx - 100, top + 234, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("全部付与"), b -> send(true))
                .bounds(cx - 32, top + 234, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("全部消す"), b -> {
            command("ench clear");
        }).bounds(cx + 36, top + 234, 64, 20).build());

        addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                .bounds(cx - 100, top + 260, 200, 20).build());

        refresh();
    }

    private void addLevel(int delta) {
        levelBox.setValue(String.valueOf(readLevel() + delta > 255 ? 255 : Math.max(1, readLevel() + delta)));
    }

    private int readLevel() {
        try {
            int v = Integer.parseInt(levelBox.getValue());
            return Math.max(1, Math.min(255, v));
        } catch (NumberFormatException e) {
            return 255;
        }
    }

    private void refresh() {
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            Button b = listButtons[i];
            if (idx < all.size()) {
                b.visible = true;
                Component name = all.get(idx).value().description();
                b.setMessage(idx == selected
                        ? Component.literal("▶ ").append(name)
                        : name);
            } else {
                b.visible = false;
            }
        }
        applyButton.active = selected >= 0;
    }

    private void send(boolean everything) {
        int lv = readLevel();
        if (everything) {
            command("ench all " + lv);
        } else if (selected >= 0 && selected < all.size()) {
            command("ench " + all.get(selected).getRegisteredName() + " " + lv);
        }
    }

    private void command(String cmd) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendCommand(cmd);
        }
    }
}
