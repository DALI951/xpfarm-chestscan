package com.dali951.xpfarmchestscan.mixin;

import com.dali951.xpfarmchestscan.scan.ChestScanner;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class OpenScreenMixin {

    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void xpfarm_captureContainerId(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (ChestScanner.isScanning()) {
            ChestScanner.expectedContainerId = packet.getContainerId();
        }
    }
}