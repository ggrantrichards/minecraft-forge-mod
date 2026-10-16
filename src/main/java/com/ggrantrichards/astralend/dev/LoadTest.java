package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

// (1.21.1 port) CI's "Load the mod in a server" step runs the game test server, which on 1.21.1
// refuses to start without a test. This one passes once the server has loaded the mod and all its data
// (in an empty 1x1x1 template).
@GameTestHolder(AstralEnd.MODID)
public final class LoadTest {
    @GameTest   // its template, by Forge's naming: data/minecraft/structure/astralend.loads.nbt, one block of air
    public static void loads(GameTestHelper helper) {
        helper.succeed();
    }

    private LoadTest() {}
}
