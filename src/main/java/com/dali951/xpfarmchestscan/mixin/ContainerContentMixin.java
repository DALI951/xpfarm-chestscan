package com.dali951.xpfarmchestscan.mixin;

import com.dali951.xpfarmchestscan.scan.ChestScanner;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundContainerSetContentPacket.class)
public class ContainerContentMixin {

    @Inject(method = "handle", at = @At("HEAD"))
    private void xpfarm_onContainerContent(ClientGamePacketListener listener, CallbackInfo ci) {
        if (!ChestScanner.isScanning()) {
            return;
        }
        ClientboundContainerSetContentPacket self = (ClientboundContainerSetContentPacket) (Object) this;
        if (ChestScanner.expectedContainerId != -1 && self.containerId() == ChestScanner.expectedContainerId) {
            ChestScanner.capturedItems = self.items();
        }
    }
}