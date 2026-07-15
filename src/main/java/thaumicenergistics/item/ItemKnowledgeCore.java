package thaumicenergistics.item;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import thaumicenergistics.util.KnowledgeCoreUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * If you're looking for methods to operate on a Knowledge Core ItemStack and its recipes, check out
 * {@link KnowledgeCoreUtil}
 *
 * @author Alex811
 */
public class ItemKnowledgeCore extends ItemMaterial {

    boolean isBlank;

    public ItemKnowledgeCore(String id, boolean isBlank) {
        super(id, 1);
        this.isBlank = isBlank;
    }

    public boolean isBlank() {
        return this.isBlank;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(
            ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        if (this.isBlank) return;

        List<KnowledgeCoreUtil.Recipe> recipes =
                KnowledgeCoreUtil.recipeStreamOf(stack).collect(Collectors.toList());
        if (recipes.isEmpty()) return;

        tooltip.add(
                TextFormatting.GRAY
                        + I18n.format(
                                "tooltip.thaumicenergistics.knowledge_core.recipes",
                                recipes.size()));
        if (GuiScreen.isShiftKeyDown()) {
            for (KnowledgeCoreUtil.Recipe recipe : recipes) {
                tooltip.add(TextFormatting.DARK_GRAY + " - " + recipe.getResult().getDisplayName());
            }
        } else {
            tooltip.add(
                    TextFormatting.DARK_GRAY
                            + I18n.format("tooltip.thaumicenergistics.knowledge_core.hold_shift"));
        }
    }
}
