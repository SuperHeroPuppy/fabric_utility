package net.supersnetwork.fabric_utility.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.supersnetwork.fabric_utility.FabricUtilityConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class FabricUtilityConfigScreen extends Screen {
    private final Screen parent;
    private final Map<String, TextFieldWidget> fields = new LinkedHashMap<>();

    protected FabricUtilityConfigScreen(Screen parent) {
        super(Text.literal("Fabric Utility Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        fields.clear();
        int y = 32;

        for (String key : FabricUtilityConfig.CONFIG_KEYS) {
            TextFieldWidget field = new TextFieldWidget(textRenderer, width / 2 - 150, y, 300, 20, Text.literal(key));
            field.setText(FabricUtilityConfig.getValue(key));
            addDrawableChild(field);
            fields.put(key, field);
            y += 34;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            fields.forEach((key, field) -> FabricUtilityConfig.setValue(key, field.getText()));
            close();
        }).dimensions(width / 2 - 154, height - 28, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 4, height - 28, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int y = 20;
        for (String key : fields.keySet()) {
            y += 34;
            context.drawTextWithShadow(textRenderer, key, width / 2 - 150, y - 30, 0xA0A0A0);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
