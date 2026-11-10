package sectorstrike;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;

import static mindustry.type.ItemStack.with;

public class SSBlocks{
    public static Block sectorArtillery;

    public static void load(){
        sectorArtillery = new StrikeTurret("sector-artillery"){{
            requirements(Category.turret, with(
                Items.copper, 300,
                Items.lead, 250,
                Items.silicon, 250,
                Items.titanium, 300,
                Items.thorium, 250,
                Items.plastanium, 200
            ));
            size = 4;
            health = 2600;
            reload = 480f;      // 8 секунд между выстрелами
            itemCapacity = 30;  // на каждый тип снаряда
        }};
    }
}
