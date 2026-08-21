package thaumicenergistics.item;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnits;
import appeng.api.implementations.items.IAEItemPowerStorage;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Exposes an {@link IAEItemPowerStorage} item's AE power store as a Forge Energy {@link
 * IEnergyStorage}, so Forge Energy chargers (Mekanism energy cubes, wireless/inventory chargers,
 * etc.) can charge it. Receive-only, mirroring AE2's own {@code PoweredItemCapabilities}: the item
 * still discharges through the AE network / wireless usage, not back out through Forge Energy.
 */
public class ForgeEnergyItemWrapper implements ICapabilityProvider, IEnergyStorage {

    private final ItemStack stack;
    private final IAEItemPowerStorage item;

    public ForgeEnergyItemWrapper(ItemStack stack, IAEItemPowerStorage item) {
        this.stack = stack;
        this.item = item;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? (T) this : null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        double convertedOffer = PowerUnits.RF.convertTo(PowerUnits.AE, maxReceive);
        double overflow =
                this.item.injectAEPower(
                        this.stack,
                        convertedOffer,
                        simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        return maxReceive - (int) PowerUnits.AE.convertTo(PowerUnits.RF, overflow);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int)
                PowerUnits.AE.convertTo(PowerUnits.RF, this.item.getAECurrentPower(this.stack));
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) PowerUnits.AE.convertTo(PowerUnits.RF, this.item.getAEMaxPower(this.stack));
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
