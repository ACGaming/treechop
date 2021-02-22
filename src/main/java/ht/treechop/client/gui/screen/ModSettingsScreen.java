package ht.treechop.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import ht.treechop.TreeChopMod;
import ht.treechop.client.Client;
import ht.treechop.client.gui.options.ExclusiveOptionRow;
import ht.treechop.client.gui.options.LabeledOptionRow;
import ht.treechop.client.gui.options.OptionList;
import ht.treechop.client.gui.util.GUIUtil;
import ht.treechop.common.settings.SneakBehavior;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.LinkedList;

public abstract class ModSettingsScreen extends Screen {

    private static final int TOP_Y = 32;
    private static final int BOTTOM_Y_OFFSET = 32;
    private static final int ROW_HEIGHT = 25;

    protected OptionList optionsRowList;
    private Button doneButton;

    public ModSettingsScreen() {
        super(new TranslationTextComponent("treechop.gui.settings.title", TreeChopMod.MOD_NAME));
    }

    @Override
    protected void init() {
        super.init();

        Collection<LabeledOptionRow> optionRows = new LinkedList<>();

        optionRows.add(
                new LabeledOptionRow(
                        font,
                        new TranslationTextComponent("treechop.gui.settings.label.chopping_enabled"),
                        new ExclusiveOptionRow.Builder()
                                .add(
                                        new TranslationTextComponent("gui.yes"),
                                        () -> Client.getChopSettings().setChoppingEnabled(true),
                                        () -> Client.getChopSettings().getChoppingEnabled()
                                )
                                .add(
                                        new TranslationTextComponent("gui.no"),
                                        () -> Client.getChopSettings().setChoppingEnabled(false),
                                        () -> !Client.getChopSettings().getChoppingEnabled()
                                )
                                .build()
                )
        );

        optionRows.add(
                new LabeledOptionRow(
                        font,
                        new TranslationTextComponent("treechop.gui.settings.label.felling_enabled"),
                        new ExclusiveOptionRow.Builder()
                                .add(
                                        new TranslationTextComponent("gui.yes"),
                                        () -> Client.getChopSettings().setFellingEnabled(true),
                                        () -> Client.getChopSettings().getFellingEnabled()
                                )
                                .add(
                                        new TranslationTextComponent("gui.no"),
                                        () -> Client.getChopSettings().setFellingEnabled(false),
                                        () -> !Client.getChopSettings().getFellingEnabled()
                                )
                                .build()
                )
        );

        optionRows.add(
                new LabeledOptionRow(
                        font,
                        new TranslationTextComponent("treechop.gui.settings.label.sneaking_affects"),
                        new ExclusiveOptionRow.Builder()
                                .add(
                                        new TranslationTextComponent("treechop.gui.settings.button.chopping"),
                                        () -> Client.getChopSettings().setSneakBehavior(SneakBehavior.INVERT_CHOPPING),
                                        () -> Client.getChopSettings().getSneakBehavior() == SneakBehavior.INVERT_CHOPPING
                                )
                                .add(
                                        new TranslationTextComponent("treechop.gui.settings.button.felling"),
                                        () -> Client.getChopSettings().setSneakBehavior(SneakBehavior.INVERT_FELLING),
                                        () -> Client.getChopSettings().getSneakBehavior() == SneakBehavior.INVERT_FELLING
                                )
                                .add(
                                        new TranslationTextComponent("treechop.gui.settings.button.nothing"),
                                        () -> Client.getChopSettings().setSneakBehavior(SneakBehavior.NONE),
                                        () -> Client.getChopSettings().getSneakBehavior() == SneakBehavior.NONE
                                )
                                .build()
                )
        );

        optionRows.add(
                new LabeledOptionRow(
                        font,
                        new TranslationTextComponent("treechop.gui.settings.label.only_chop_trees_with_leaves"),
                        new ExclusiveOptionRow.Builder()
                                .add(
                                        new TranslationTextComponent("gui.yes"),
                                        () -> Client.getChopSettings().setTreesMustHaveLeaves(true),
                                        () -> Client.getChopSettings().getTreesMustHaveLeaves()
                                )
                                .add(
                                        new TranslationTextComponent("gui.no"),
                                        () -> Client.getChopSettings().setTreesMustHaveLeaves(false),
                                        () -> !Client.getChopSettings().getTreesMustHaveLeaves()
                                )
                                .build()
                )
        );

        optionRows.add(
                new LabeledOptionRow(
                        font,
                        new TranslationTextComponent("treechop.gui.settings.label.chop_in_creative_mode"),
                        new ExclusiveOptionRow.Builder()
                                .add(
                                        new TranslationTextComponent("gui.yes"),
                                        () -> Client.getChopSettings().setChopInCreativeMode(true),
                                        () -> Client.getChopSettings().getChopInCreativeMode()
                                )
                                .add(
                                        new TranslationTextComponent("gui.no"),
                                        () -> Client.getChopSettings().setChopInCreativeMode(false),
                                        () -> !Client.getChopSettings().getChopInCreativeMode()
                                )
                                .build()
                )
        );

        this.optionsRowList = addListener(new OptionList(
                minecraft,
                width,
                height,
                TOP_Y,
                height - BOTTOM_Y_OFFSET,
                ROW_HEIGHT,
                optionRows
        ));

        final int doneButtonWidth = 200;
        doneButton = addButton(new Button(
                (width - doneButtonWidth) / 2,
                getDoneButtonTop(),
                doneButtonWidth,
                GUIUtil.BUTTON_HEIGHT,
                ITextComponent.getTextComponentOrEmpty(I18n.format("gui.done")),
                button -> closeScreen()
        ));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        doneButton.y = getDoneButtonTop();

        this.renderBackground(matrixStack);
        optionsRowList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, getTitleTop(), 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        // TODO: check out ModSettingsScreen.func_243293_a for draw reordering; might be important for tooltips
    }

    protected int getDoneButtonTop() {
        return height - 26;
    }

    protected int getTitleTop() {
        return 20;
    }
}
