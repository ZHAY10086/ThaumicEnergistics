package thaumicenergistics.network.packets;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import thaumicenergistics.container.item.ContainerWirelessEssentiaTerminal;

public class PacketWirelessTerminalPowerUpdate implements IMessage {

    public boolean powered;
    public boolean active;

    public PacketWirelessTerminalPowerUpdate() {}

    public PacketWirelessTerminalPowerUpdate(boolean powered, boolean active) {
        this.powered = powered;
        this.active = active;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.powered = buf.readBoolean();
        this.active = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.powered);
        buf.writeBoolean(this.active);
    }

    public static class Handler
            implements IMessageHandler<PacketWirelessTerminalPowerUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketWirelessTerminalPowerUpdate message, MessageContext ctx) {
            FMLCommonHandler.instance()
                    .getWorldThread(ctx.netHandler)
                    .addScheduledTask(
                            () -> {
                                GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                                if (!(screen instanceof GuiContainer)) return;
                                Container container = ((GuiContainer) screen).inventorySlots;
                                if (container instanceof ContainerWirelessEssentiaTerminal) {
                                    ((ContainerWirelessEssentiaTerminal) container)
                                            .setSyncedPowerState(message.powered, message.active);
                                }
                            });
            return null;
        }
    }
}
