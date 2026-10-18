package com.castra.jeihistory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JeiHistory.MODID)
public class JeiHistory {
    public static final String MODID = "jeihistory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public JeiHistory() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, JeiHistoryConfig.SPEC);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(JeiHistoryConfig::onConfigEvent);
        }
    }
}
