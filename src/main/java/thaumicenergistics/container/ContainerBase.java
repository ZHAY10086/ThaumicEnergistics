package thaumicenergistics.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import thaumicenergistics.container.slot.*;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketInvHeldUpdate;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.util.EssentiaFilter;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ItemHandlerUtil;

import java.util.function.Predicate;

/**
 * The base container for all containers in Thaumic Energistics
 *
 * <p>
 *
 * @author BrockWS
 */
public abstract class ContainerBase extends Container {

    public EntityPlayer player;

    /**
     * Bounds [start, end) of the player-inventory slot block, recorded by {@link
     * #bindPlayerInventory}.
     */
    protected int playerSlotStart = -1;

    protected int playerSlotEnd = -1;

    public ContainerBase(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Two-way shift-click routing for a dedicated single-item slot (knowledge core, discount gear,
     * ...). A stack matching {@code accepts} in any other slot is routed INTO the first empty,
     * valid {@code destSlotType} slot; shift-clicking the dedicated slot itself sends its contents
     * to the player inventory (never into the ME network). Returns the (empty) transfer result when
     * it handled the click, or {@code null} to signal "not mine - fall through to the default
     * transfer".
     */
    protected ItemStack routeDedicatedSlot(
            int index, Class<? extends Slot> destSlotType, Predicate<ItemStack> accepts) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;

        // OUT: shift-click the dedicated slot -> player inventory, bypassing the ME network.
        if (destSlotType.isInstance(slot)) {
            ItemStack moving = slot.getStack().copy();
            if (this.playerSlotStart >= 0
                    && this.mergeItemStack(
                            moving, this.playerSlotStart, this.playerSlotEnd, true)) {
                slot.putStack(moving.isEmpty() ? ItemStack.EMPTY : moving);
                slot.onSlotChanged();
                this.detectAndSendChanges();
            }
            return ItemStack.EMPTY;
        }

        // IN: shift-click a matching item elsewhere -> the dedicated slot.
        if (accepts.test(slot.getStack())) {
            for (Slot target : this.inventorySlots) {
                if (!destSlotType.isInstance(target) || target.getHasStack()) continue;
                if (!target.isItemValid(slot.getStack())) continue;
                ItemStack one = slot.getStack().copy();
                one.setCount(1);
                target.putStack(one);
                slot.decrStackSize(1);
                this.detectAndSendChanges();
                return ItemStack.EMPTY;
            }
        }
        return null;
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotID < 0) return super.slotClick(slotID, dragType, clickType, player);
        if (slotID >= this.inventorySlots.size()) return ItemStack.EMPTY;

        Slot slot = this.getSlot(slotID);
        if (slot instanceof SlotGhostEssentia) {
            if (((SlotGhostEssentia) slot).getFilter() != null) {
                EssentiaFilter filter = ((SlotGhostEssentia) slot).getFilter();
                ItemStack stack = player.inventory.getItemStack().copy();
                int id = slot.getSlotIndex();

                if (stack.getItem() instanceof IEssentiaContainerItem) {
                    IEssentiaContainerItem item = (IEssentiaContainerItem) stack.getItem();
                    if (item.getAspects(stack) != null) {
                        AspectList aspects = item.getAspects(stack);
                        filter.setAspect(aspects.getAspects()[0], id);
                    }
                } else {
                    filter.setAspect(null, id);
                }
                return ItemStack.EMPTY;
            }
        }
        if (slot instanceof SlotGhost) {
            ItemStack stack = player.inventory.getItemStack().copy();
            stack.setCount(1);
            slot.putStack(stack);
            return ItemStack.EMPTY;
        }
        if (slot instanceof SlotArcaneResult && this instanceof ICraftingContainer) {
            ICraftingContainer craftingContainer = ((ICraftingContainer) this);
            ItemStack held = player.inventory.getItemStack();
            if (ForgeUtil.isServer()
                    && (held.isEmpty() || slot.getStack().isItemEqual(held))
                    && (clickType == ClickType.QUICK_MOVE
                            || slot.getStack().getMaxStackSize() - held.getCount()
                                    >= slot.getStack().getCount())) {
                int numToCraft =
                        clickType == ClickType.QUICK_MOVE
                                ? Integer.MAX_VALUE
                                : 1; // if quick move, calc max craftable amount, else craft 1
                int canCraftNum =
                        craftingContainer.tryCraft(numToCraft); // we can craft this amount
                if (canCraftNum > 0) {
                    ItemStack toCraft = slot.getStack().copy();
                    toCraft.setCount(canCraftNum);
                    if (clickType == ClickType.QUICK_MOVE) {
                        int canFitInInvNum =
                                canCraftNum
                                        - ForgeUtil.addStackToPlayerInventory(player, toCraft, true)
                                                .getCount(); // check how much fits in the player's
                        // inventory
                        if (canFitInInvNum < canCraftNum)
                            toCraft.setCount(
                                    canFitInInvNum); // if it doesn't fit, craft as much as we can
                        // fit
                        ItemStack newToStore = craftingContainer.onCraft(toCraft);
                        ForgeUtil.addStackToPlayerInventory(player, newToStore, false);
                    } else {
                        ItemStack newHeld = craftingContainer.onCraft(toCraft);
                        newHeld.grow(held.getCount());
                        player.inventory.setItemStack(newHeld);
                        PacketHandler.sendToPlayer(
                                (EntityPlayerMP) player, new PacketInvHeldUpdate(newHeld));
                    }
                }
            }
            return ItemStack.EMPTY;
        }
        if (!(this instanceof ContainerBaseTerminal) && clickType == ClickType.QUICK_MOVE) {
            if (slot instanceof SlotUpgrade || slot instanceof SlotKnowledgeCore)
                ItemHandlerUtil.quickMoveSlot(
                        new InvWrapper(this.player.inventory), slot, false, true);
            else handleQuickMove(slot, slot.getStack());
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotID, dragType, clickType, player);
    }

    protected void handleQuickMove(Slot slot, ItemStack itemStack) {}

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    protected void bindPlayerArmour(
            EntityPlayer player, IItemHandler inv, int offsetX, int offsetY) {
        this.addSlotToContainer(new SlotArmor(player, inv, 0, offsetX, offsetY + 8 + 18 * 3));
        this.addSlotToContainer(new SlotArmor(player, inv, 1, offsetX, offsetY + 8 + 18 * 2));
        this.addSlotToContainer(new SlotArmor(player, inv, 2, offsetX, offsetY + 8 + 18));
        this.addSlotToContainer(new SlotArmor(player, inv, 3, offsetX, offsetY + 8));
    }

    protected void bindPlayerInventory(IItemHandler player, int offsetX, int offsetY) {
        this.playerSlotStart = this.inventorySlots.size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(
                        new ThESlot(
                                player, 9 * i + j + 9, offsetX + 8 + 18 * j, offsetY + 2 + 18 * i));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new ThESlot(player, i, offsetX + 8 + 18 * i, offsetY + 60));
        }
        this.playerSlotEnd = this.inventorySlots.size();
    }

    /**
     * Called when a PacketUIAction is received by the server
     *
     * @param player Player that sent the action
     * @param packet Packet from client
     */
    public void onAction(EntityPlayerMP player, PacketUIAction packet) {}

    public EssentiaFilter getEssentiaFilter() {
        return null;
    }

    public void setEssentiaFilter(EssentiaFilter filter) {
        this.getEssentiaFilter().deserializeNBT(filter.serializeNBT());
    }

    public void handleJEITransfer(EntityPlayer player, NBTTagCompound tag) {}

    @Override
    public boolean canMergeSlot(
            ItemStack stack, Slot slotIn) { // prevent stack merging (double-click) here
        if (slotIn instanceof SlotME || slotIn instanceof SlotArcaneResult) return false;
        return super.canMergeSlot(stack, slotIn);
    }
}
