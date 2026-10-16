package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

// (1.21.1 port) CI's "Load the mod in a server" step runs the game test server, which on 1.21.1
// refuses to start without a test. This one passes once the server has loaded the mod and all its data
// (it stands in one of the mod's own structure templates).
@GameTestHolder(AstralEnd.MODID)
@PrefixGameTestTemplate(false)
public final class LoadTest {
    @GameTest(template = "astralend:comet_crater_1")
    public static void loads(GameTestHelper helper) {
        helper.succeed();
    }

    private LoadTest() {}
}
