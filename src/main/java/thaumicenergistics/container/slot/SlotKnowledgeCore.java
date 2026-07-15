package thaumicenergistics.container.slot;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import thaumicenergistics.ThaumicEnergisticsApi;
import thaumicenergistics.item.ItemKnowledgeCore;

import javax.annotation.Nullable;

/**
 * @author Alex811
 */
public class SlotKnowledgeCore extends ThESlot {
    public SlotKnowledgeCore(IItemHandler handler, int index, int xPosition, int yPosition) {
        super(handler, index, xPosition, yPosition);
    }

    /**
     * This is the dedicated core slot, so always accept a knowledge core. The backing upgrade
     * inventory rejects a core once one is installed, which is correct for insertion but blocks
     * vanilla's swap-click - that gate also relies on isItemValid. Since there is only ever one
     * physical slot, accepting here can't over-fill; it just lets a core on the cursor swap with
     * the one already present.
     */
    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.getItem() instanceof ItemKnowledgeCore) return true;
        return super.isItemValid(stack);
    }

    @Nullable
    @Override
    public String getSlotTexture() {
        return ThaumicEnergisticsApi.instance().textures().knowledgeCoreSlot().toString();
    }
}
