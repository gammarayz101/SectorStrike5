package sectorstrike;

import arc.Events;
import mindustry.content.*;
import mindustry.content.TechTree.TechNode;
import mindustry.game.EventType.*;
import mindustry.game.Objectives;
import mindustry.mod.Mod;
import mindustry.ctype.UnlockableContent;
import mindustry.type.ItemStack;

public class SectorStrikeMod extends Mod{

    public SectorStrikeMod(){
        Events.on(ClientLoadEvent.class, e -> addTech());
        Events.on(WorldLoadEvent.class, e -> Strikes.onWorldLoad());
    }

    @Override
    public void loadContent(){
        SSBlocks.load();
    }

    static TechNode find(UnlockableContent c){
        for(TechNode n : TechTree.all){
            if(n.content == c) return n;
        }
        return null;
    }

    void addTech(){
        if(find(SSBlocks.sectorArtillery) != null) return;
        TechNode parent = find(Blocks.ripple);
        if(parent == null) return;
        TechNode node = new TechNode(parent, SSBlocks.sectorArtillery, ItemStack.with(
            Items.copper, 3000,
            Items.lead, 3000,
            Items.silicon, 2500,
            Items.titanium, 2500,
            Items.thorium, 1500,
            Items.plastanium, 1000
        ));
        node.objectives.add(new Objectives.Research(Blocks.thoriumReactor));
    }
}
