package com.castra.jeihistory;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class JeiHistoryConfig {
    private static final ForgeConfigSpec.BooleanValue OPEN_HISTORY;
    private static final ForgeConfigSpec.IntValue HISTORY_ROWS;

    public static final ForgeConfigSpec SPEC;

    private static volatile boolean openHistory = true;
    private static volatile int historyRows = 2;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("client-settings");
        OPEN_HISTORY = builder
            .comment("Whether to show the JEI recipe lookup history")
            .define("open_history", true);
        HISTORY_ROWS = builder
            .comment("Number of history rows (1-10)")
            .defineInRange("history_rows", 2, 1, 10);
        builder.pop();
        SPEC = builder.build();
    }

    private JeiHistoryConfig() {
    }

    public static boolean openHistory() {
        return openHistory;
    }

    public static int historyRows() {
        return historyRows;
    }

    public static void onConfigEvent(ModConfigEvent event) {
        if (JeiHistory.MODID.equals(event.getConfig().getModId())) {
            openHistory = OPEN_HISTORY.get();
            historyRows = HISTORY_ROWS.get();
        }
    }
}
