package thaumicenergistics.integration.appeng;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IStorageChannel;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import thaumcraft.api.aspects.Aspect;

import thaumicenergistics.api.EssentiaStack;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;
import thaumicenergistics.item.ItemDummyAspect;

import java.util.Locale;

/**
 * @author BrockWS
 */
public class AEEssentiaStack implements IAEEssentiaStack, Comparable<AEEssentiaStack> {

    private Aspect aspect;
    private long stackSize;
    private long countRequestable;
    private boolean isCraftable;
    private int hash;

    private AEEssentiaStack(Aspect aspect, long amount) {
        this.aspect = aspect;
        if (this.aspect == null) {
            throw new IllegalArgumentException("Aspect is null");
        }
        this.setStackSize(amount);
        this.setCraftable(false);
        this.setCountRequestable(0);
        // Derive the hash from the (normalized) aspect tag so it's consistent with the
        // case-insensitive tag equality and stable across JVM runs - rather than the Aspect's
        // identity hash, which Thaumcraft leaves as Object's and which changes every run.
        this.hash = this.aspect.getTag().toLowerCase(Locale.ROOT).hashCode();
    }

    private AEEssentiaStack(AEEssentiaStack stack) {
        this.aspect = stack.getAspect();
        if (this.aspect == null) throw new IllegalArgumentException("Aspect is null");
        this.setStackSize(stack.getStackSize());
        this.setCraftable(false);
        this.setCountRequestable(0);
        this.hash = stack.hash;
    }

    public static AEEssentiaStack fromEssentiaStack(EssentiaStack stack) {
        if (stack == null) return null;
        return new AEEssentiaStack(stack.getAspect(), stack.getAmount());
    }

    public static IAEEssentiaStack fromNBT(NBTTagCompound t) {
        EssentiaStack stack = EssentiaStack.readFromNBT(t);
        if (stack == null) return null;
        AEEssentiaStack ae = AEEssentiaStack.fromEssentiaStack(stack);
        ae.setCountRequestable(t.getLong("Req"));
        ae.setCraftable(t.getBoolean("Craft"));
        return ae;
    }

    public static IAEEssentiaStack fromPacket(ByteBuf buf) {
        return AEEssentiaStack.fromNBT(ByteBufUtils.readTag(buf));
    }

    @Override
    public long getStackSize() {
        return this.stackSize;
    }

    @Override
    public IAEEssentiaStack setStackSize(long l) {
        this.stackSize = l;
        return this;
    }

    @Override
    public long getCountRequestable() {
        return this.countRequestable;
    }

    @Override
    public IAEEssentiaStack setCountRequestable(long l) {
        this.countRequestable = l;
        return this;
    }

    @Override
    public boolean isCraftable() {
        return this.isCraftable;
    }

    @Override
    public IAEEssentiaStack setCraftable(boolean b) {
        this.isCraftable = b;
        return this;
    }

    @Override
    public IAEEssentiaStack reset() {
        this.setStackSize(0);
        this.setCountRequestable(0);
        this.setCraftable(false);
        return this;
    }

    @Override
    public boolean isMeaningful() {
        return (this.getAspect() != null && this.getStackSize() != 0)
                || this.countRequestable > 0
                || this.isCraftable;
    }

    @Override
    public void incStackSize(long l) {
        this.setStackSize(this.getStackSize() + l);
    }

    @Override
    public void decStackSize(long l) {
        this.setStackSize(this.getStackSize() - l);
    }

    @Override
    public void incCountRequestable(long l) {
        this.setCountRequestable(this.getCountRequestable() + l);
    }

    @Override
    public void decCountRequestable(long l) {
        this.setCountRequestable(this.getCountRequestable() - l);
    }

    @Override
    public Aspect getAspect() {
        return this.aspect;
    }

    @Override
    public EssentiaStack getStack() {
        return new EssentiaStack(this.getAspect(), this.stackSize);
    }

    @Override
    public void add(IAEEssentiaStack option) {
        if (option == null) return;
        this.incStackSize(option.getStackSize());
        this.setCountRequestable(this.getCountRequestable() + option.getCountRequestable());
        this.setCraftable(this.isCraftable() || option.isCraftable());
    }

    @Override
    public void writeToNBT(NBTTagCompound t) {
        t.setString("Aspect", this.getAspect().getTag());
        t.setByte("Count", (byte) 0);
        t.setLong("Amount", this.getStackSize());
        t.setLong("Req", this.getCountRequestable());
        t.setBoolean("Craft", this.isCraftable());
    }

    @Override
    public void writeToPacket(ByteBuf buf) {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        ByteBufUtils.writeTag(buf, tag);
    }

    @Override
    public IAEEssentiaStack copy() {
        return new AEEssentiaStack(this);
    }

    @Override
    public IAEEssentiaStack empty() {
        IAEEssentiaStack copy = this.copy();
        copy.reset();
        return copy;
    }

    @Override
    public boolean isItem() {
        return false;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public IStorageChannel<IAEEssentiaStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class);
    }

    @Override
    public ItemStack asItemStackRepresentation() {
        // TODO: Test
        ItemStack stack =
                ThEApi.instance().items().dummyAspect().maybeStack(1).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) ((ItemDummyAspect) stack.getItem()).setAspect(stack, this.aspect);
        return stack;
    }

    @Override
    public boolean fuzzyComparison(IAEEssentiaStack other, FuzzyMode mode) {
        return this.aspect == other.getAspect();
    }

    @Override
    public int compareTo(AEEssentiaStack o) {
        return Integer.compare(this.hashCode(), o.hashCode());
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        // Must not also match plain EssentiaStack: it never overrides equals(), so a cross-type
        // match here would be asymmetric and violate the Object.equals contract.
        if (!(obj instanceof AEEssentiaStack)) return false;
        return this.getAspect()
                .getTag()
                .equalsIgnoreCase(((AEEssentiaStack) obj).getAspect().getTag());
    }
}
