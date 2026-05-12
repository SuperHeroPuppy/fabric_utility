package net.supersnetwork.fabric_utility.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.supersnetwork.fabric_utility.FabricUtilityConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FabricUtilityConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int SECTION_GAP = 18;
    private static final int FIELD_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 34;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<ListValue> blockedEntities = new ArrayList<>();
    private final List<ListValue> soundSuffixes = new ArrayList<>();
    private final List<CustomSoundValue> customSounds = new ArrayList<>();
    private int scrollY;
    private int contentHeight;
    private TextFieldWidget maxPlayerParticles;
    private TextFieldWidget defaultSound;
    private TextFieldWidget defaultVolume;
    private TextFieldWidget defaultPitch;
    private ButtonWidget nicknameToggle;
    private boolean nicknameEnabled;

    protected FabricUtilityConfigScreen(Screen parent) {
        super(Text.literal("Fabric Utility Config"));
        this.parent = parent;
        readConfig();
    }

    @Override
    protected void init() {
        rebuildRows();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(0, 0, width, 28, 0xCC101015);
        context.fill(0, height - FOOTER_HEIGHT, width, height, 0xCC101015);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        int top = 32;
        int bottom = height - FOOTER_HEIGHT - 6;
        context.enableScissor(0, top, width, bottom);

        for (Row row : rows) {
            int y = row.y - scrollY;
            if (y > -32 && y < height) {
                row.render(context, width / 2 - 180, y, width);
            }
        }

        context.disableScissor();
        super.render(context, mouseX, mouseY, delta);
        drawScrollbar(context, top, bottom);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, contentHeight - (height - FOOTER_HEIGHT - 42));
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * 18)));
        positionRows();
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void rebuildRows() {
        clearChildren();
        rows.clear();

        addSection("General");
        nicknameToggle = ButtonWidget.builder(toggleText("Nicknames", nicknameEnabled), button -> {
            nicknameEnabled = !nicknameEnabled;
            button.setMessage(toggleText("Nicknames", nicknameEnabled));
        }).dimensions(0, 0, 160, FIELD_HEIGHT).build();
        addRow(new ControlRow("Nickname system", "Applies nicknames to chat and game messages.", nicknameToggle));

        addSection("Petting");
        maxPlayerParticles = textField(FabricUtilityConfig.getValue("maxPlayerPetParticles"));
        addRow(new FieldRow("Max player particles", "Caps heart particles when petting players.", maxPlayerParticles));
        defaultSound = textField(FabricUtilityConfig.getValue("defaultPlayerPetSound"));
        addRow(new FieldRow("Default player sound", "Sound id used for players and fallback pet sounds.", defaultSound));
        defaultVolume = textField(FabricUtilityConfig.getValue("defaultPlayerPetVolume"));
        defaultPitch = textField(FabricUtilityConfig.getValue("defaultPlayerPetPitch"));
        addRow(new TwoFieldRow("Default volume", defaultVolume, "Default pitch", defaultPitch));

        addListSection("Blocked Pettable Entities", "Entity ids that cannot be pet.", blockedEntities, () -> {
            blockedEntities.add(new ListValue(""));
            rebuildRows();
        });

        addListSection("Entity Sound Suffixes", "Tried in order as entity.<entity>.<suffix>.", soundSuffixes, () -> {
            soundSuffixes.add(new ListValue(""));
            rebuildRows();
        });

        addSection("Custom Tag Sounds");
        addRow(new HelpRow("When an entity has one of these command tags, this sound is used for petting."));
        for (CustomSoundValue sound : customSounds) {
            TextFieldWidget tag = textField(sound.tag);
            TextFieldWidget soundId = textField(sound.soundId);
            TextFieldWidget volume = textField(sound.volume);
            TextFieldWidget pitch = textField(sound.pitch);
            ButtonWidget remove = ButtonWidget.builder(Text.literal("Remove"), button -> {
                customSounds.remove(sound);
                rebuildRows();
            }).dimensions(0, 0, 70, FIELD_HEIGHT).build();
            addRow(new CustomSoundRow(sound, tag, soundId, volume, pitch, remove));
        }
        addRow(new ButtonRow(ButtonWidget.builder(Text.literal("Add custom sound"), button -> {
            customSounds.add(new CustomSoundValue("petting_purr", "minecraft:entity.cat.purr", "0.7", "1.0"));
            rebuildRows();
        }).dimensions(0, 0, 160, FIELD_HEIGHT).build()));

        addFooterButtons();
        positionRows();
    }

    private void addListSection(String title, String help, List<ListValue> values, Runnable addAction) {
        addSection(title);
        addRow(new HelpRow(help));

        for (ListValue value : values) {
            TextFieldWidget field = textField(value.value);
            ButtonWidget remove = ButtonWidget.builder(Text.literal("Remove"), button -> {
                values.remove(value);
                rebuildRows();
            }).dimensions(0, 0, 70, FIELD_HEIGHT).build();
            addRow(new ListRow(value, field, remove));
        }

        addRow(new ButtonRow(ButtonWidget.builder(Text.literal("Add"), button -> addAction.run())
                .dimensions(0, 0, 90, FIELD_HEIGHT)
                .build()));
    }

    private void addFooterButtons() {
        ButtonWidget save = ButtonWidget.builder(Text.literal("Save"), button -> {
            writeConfig();
            close();
        }).dimensions(width / 2 - 154, height - 27, 150, 20).build();
        ButtonWidget cancel = ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 4, height - 27, 150, 20)
                .build();
        addDrawableChild(save);
        addDrawableChild(cancel);
    }

    private void addSection(String title) {
        addRow(new SectionRow(title));
    }

    private void addRow(Row row) {
        rows.add(row);
        row.children().forEach(this::addDrawableChild);
    }

    private TextFieldWidget textField(String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, 180, FIELD_HEIGHT, Text.empty());
        field.setText(value);
        field.setMaxLength(512);
        return field;
    }

    private void positionRows() {
        int y = 38;
        int left = width / 2 - 180;
        int right = width / 2 + 180;

        for (Row row : rows) {
            row.y = y;
            row.position(left, y - scrollY, right);
            y += row.height();
        }

        contentHeight = y;
    }

    private void drawScrollbar(DrawContext context, int top, int bottom) {
        int viewport = bottom - top;
        int maxScroll = Math.max(0, contentHeight - viewport);

        if (maxScroll <= 0) {
            return;
        }

        int barHeight = Math.max(24, viewport * viewport / contentHeight);
        int barY = top + (viewport - barHeight) * scrollY / maxScroll;
        context.fill(width - 7, top, width - 4, bottom, 0x66000000);
        context.fill(width - 7, barY, width - 4, barY + barHeight, 0xFFAAAAAA);
    }

    private void readConfig() {
        nicknameEnabled = Boolean.parseBoolean(FabricUtilityConfig.getValue("nicknameSystemEnabled"));
        splitList(FabricUtilityConfig.getValue("blockedPettableEntities")).forEach(value -> blockedEntities.add(new ListValue(value)));
        splitList(FabricUtilityConfig.getValue("pettingSoundSuffixes")).forEach(value -> soundSuffixes.add(new ListValue(value)));
        splitList(FabricUtilityConfig.getValue("customPetSounds"), ";").forEach(value -> customSounds.add(CustomSoundValue.parse(value)));
    }

    private void writeConfig() {
        rows.forEach(Row::sync);
        FabricUtilityConfig.setValue("nicknameSystemEnabled", Boolean.toString(nicknameEnabled));
        FabricUtilityConfig.setValue("maxPlayerPetParticles", maxPlayerParticles.getText());
        FabricUtilityConfig.setValue("defaultPlayerPetSound", defaultSound.getText());
        FabricUtilityConfig.setValue("defaultPlayerPetVolume", defaultVolume.getText());
        FabricUtilityConfig.setValue("defaultPlayerPetPitch", defaultPitch.getText());
        FabricUtilityConfig.setValue("blockedPettableEntities", joinList(blockedEntities));
        FabricUtilityConfig.setValue("pettingSoundSuffixes", joinList(soundSuffixes));
        FabricUtilityConfig.setValue("customPetSounds", joinCustomSounds());
    }

    private List<String> splitList(String value) {
        return splitList(value, ",");
    }

    private List<String> splitList(String value, String separator) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(separator))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private String joinList(List<ListValue> values) {
        return values.stream()
                .map(value -> value.value)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String joinCustomSounds() {
        return customSounds.stream()
                .filter(CustomSoundValue::isNotBlank)
                .map(CustomSoundValue::serialize)
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private Text toggleText(String label, boolean enabled) {
        return Text.literal(label + ": " + (enabled ? "On" : "Off"));
    }

    private abstract class Row {
        private int y;

        abstract int height();

        abstract void render(DrawContext context, int left, int y, int screenWidth);

        void position(int left, int y, int right) {
        }

        void sync() {
        }

        List<? extends ClickableWidget> children() {
            return List.of();
        }
    }

    private class SectionRow extends Row {
        private final String title;

        private SectionRow(String title) {
            this.title = title;
        }

        @Override
        int height() {
            return 30;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
            context.drawTextWithShadow(textRenderer, title, left, y + 8, 0xFFD6B56D);
            context.fill(left, y + 24, left + 360, y + 25, 0x55FFFFFF);
        }
    }

    private class HelpRow extends Row {
        private final String text;

        private HelpRow(String text) {
            this.text = text;
        }

        @Override
        int height() {
            return 18;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
            context.drawTextWithShadow(textRenderer, text, left, y + 2, 0xFF9CA3AF);
        }
    }

    private class FieldRow extends Row {
        private final String label;
        private final String help;
        private final TextFieldWidget field;

        private FieldRow(String label, String help, TextFieldWidget field) {
            this.label = label;
            this.help = help;
            this.field = field;
        }

        @Override
        int height() {
            return 44;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
            context.drawTextWithShadow(textRenderer, label, left, y + 1, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, help, left, y + 13, 0xFF9CA3AF);
        }

        @Override
        void position(int left, int y, int right) {
            field.setX(right - 180);
            field.setY(y + 9);
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(field);
        }
    }

    private class TwoFieldRow extends Row {
        private final String leftLabel;
        private final TextFieldWidget leftField;
        private final String rightLabel;
        private final TextFieldWidget rightField;

        private TwoFieldRow(String leftLabel, TextFieldWidget leftField, String rightLabel, TextFieldWidget rightField) {
            this.leftLabel = leftLabel;
            this.leftField = leftField;
            this.rightLabel = rightLabel;
            this.rightField = rightField;
        }

        @Override
        int height() {
            return 34;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
            context.drawTextWithShadow(textRenderer, leftLabel, left, y + 7, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, rightLabel, left + 182, y + 7, 0xFFFFFFFF);
        }

        @Override
        void position(int left, int y, int right) {
            leftField.setWidth(72);
            rightField.setWidth(72);
            leftField.setX(left + 102);
            leftField.setY(y + 2);
            rightField.setX(right - 72);
            rightField.setY(y + 2);
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(leftField, rightField);
        }
    }

    private class ControlRow extends FieldRow {
        private final ButtonWidget button;

        private ControlRow(String label, String help, ButtonWidget button) {
            super(label, help, textField(""));
            this.button = button;
        }

        @Override
        void position(int left, int y, int right) {
            button.setX(right - 160);
            button.setY(y + 9);
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(button);
        }
    }

    private class ListRow extends Row {
        private final ListValue value;
        private final TextFieldWidget field;
        private final ButtonWidget remove;

        private ListRow(ListValue value, TextFieldWidget field, ButtonWidget remove) {
            this.value = value;
            this.field = field;
            this.remove = remove;
        }

        @Override
        int height() {
            return ROW_HEIGHT;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
        }

        @Override
        void position(int left, int y, int right) {
            field.setWidth(282);
            field.setX(left);
            field.setY(y + 1);
            remove.setX(right - 70);
            remove.setY(y + 1);
        }

        @Override
        void sync() {
            value.value = field.getText();
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(field, remove);
        }
    }

    private class CustomSoundRow extends Row {
        private final CustomSoundValue value;
        private final TextFieldWidget tag;
        private final TextFieldWidget sound;
        private final TextFieldWidget volume;
        private final TextFieldWidget pitch;
        private final ButtonWidget remove;

        private CustomSoundRow(CustomSoundValue value, TextFieldWidget tag, TextFieldWidget sound, TextFieldWidget volume, TextFieldWidget pitch, ButtonWidget remove) {
            this.value = value;
            this.tag = tag;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.remove = remove;
        }

        @Override
        int height() {
            return 48;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
            context.drawTextWithShadow(textRenderer, "Tag", left, y, 0xFF9CA3AF);
            context.drawTextWithShadow(textRenderer, "Sound", left + 104, y, 0xFF9CA3AF);
            context.drawTextWithShadow(textRenderer, "Vol", left + 232, y, 0xFF9CA3AF);
            context.drawTextWithShadow(textRenderer, "Pitch", left + 282, y, 0xFF9CA3AF);
        }

        @Override
        void position(int left, int y, int right) {
            tag.setWidth(96);
            sound.setWidth(122);
            volume.setWidth(44);
            pitch.setWidth(44);
            tag.setX(left);
            sound.setX(left + 104);
            volume.setX(left + 232);
            pitch.setX(left + 282);
            remove.setX(right - 70);
            tag.setY(y + 16);
            sound.setY(y + 16);
            volume.setY(y + 16);
            pitch.setY(y + 16);
            remove.setY(y + 16);
        }

        @Override
        void sync() {
            value.tag = tag.getText();
            value.soundId = sound.getText();
            value.volume = volume.getText();
            value.pitch = pitch.getText();
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(tag, sound, volume, pitch, remove);
        }
    }

    private class ButtonRow extends Row {
        private final ButtonWidget button;

        private ButtonRow(ButtonWidget button) {
            this.button = button;
        }

        @Override
        int height() {
            return FIELD_HEIGHT + SECTION_GAP;
        }

        @Override
        void render(DrawContext context, int left, int y, int screenWidth) {
        }

        @Override
        void position(int left, int y, int right) {
            button.setX(left);
            button.setY(y);
        }

        @Override
        List<? extends ClickableWidget> children() {
            return List.of(button);
        }
    }

    private static final class ListValue {
        private String value;

        private ListValue(String value) {
            this.value = value;
        }
    }

    private static final class CustomSoundValue {
        private String tag;
        private String soundId;
        private String volume;
        private String pitch;

        private CustomSoundValue(String tag, String soundId, String volume, String pitch) {
            this.tag = tag;
            this.soundId = soundId;
            this.volume = volume;
            this.pitch = pitch;
        }

        private static CustomSoundValue parse(String value) {
            String[] tagAndSound = value.split("=", 2);
            if (tagAndSound.length != 2) {
                return new CustomSoundValue("", "", "0.8", "1.0");
            }

            String[] soundParts = tagAndSound[1].split(":");
            String soundId = soundParts.length >= 2 ? soundParts[0] + ":" + soundParts[1] : "";
            String volume = soundParts.length >= 3 ? soundParts[2] : "0.8";
            String pitch = soundParts.length >= 4 ? soundParts[3] : "1.0";
            return new CustomSoundValue(tagAndSound[0], soundId, volume, pitch);
        }

        private boolean isNotBlank() {
            return !tag.isBlank() && !soundId.isBlank();
        }

        private String serialize() {
            return tag + "=" + soundId + ":" + volume + ":" + pitch;
        }
    }
}
