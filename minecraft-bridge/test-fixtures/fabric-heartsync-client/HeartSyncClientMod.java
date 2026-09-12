package com.example.heartsync.client;

import net.fabricmc.api.ModInitializer;

/**
 * Demo Fabric CLIENT-style fixture: implements the main entrypoint
 * (net.fabricmc.api.ModInitializer) rather than the dedicated-server
 * variant. Exercises the adapter's other remap path
 * (net/fabricmc/api/ModInitializer -> com/rnk/shim/ModInitializer).
 */
public class HeartSyncClientMod implements ModInitializer {
    public HeartSyncClientMod() {
        System.out.println("[HeartSyncClient] constructor: real Fabric mod code executing");
    }

    @Override
    public void onInitialize() {
        System.out.println("[HeartSyncClient] onInitialize: Fabric ModInitializer contract reached");
        System.setProperty("rnk.verify.com.example.heartsync.client.HeartSyncClientMod", "executed");
        System.out.println("[HeartSyncClient] side effect recorded (system property set)");
    }
}
