package com.example.heartsync;

import net.fabricmc.api.DedicatedServerModInitializer;

/**
 * Demo Fabric server-side mod (fixture). Real code path: the constructor
 * runs, onInitializeServer() runs (the REAL DedicatedServerModInitializer
 * contract), and a side effect is recorded so an external harness can prove
 * execution.
 */
public class HeartSyncMod implements DedicatedServerModInitializer {
    public HeartSyncMod() {
        System.out.println("[HeartSync] constructor: real Fabric mod code executing");
    }

    @Override
    public void onInitializeServer() {
        System.out.println("[HeartSync] onInitializeServer: Fabric DedicatedServerModInitializer contract reached");
        System.setProperty("rnk.verify.com.example.heartsync.HeartSyncMod", "executed");
        System.out.println("[HeartSync] side effect recorded (system property set)");
    }
}
