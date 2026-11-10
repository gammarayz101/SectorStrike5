package sectorstrike;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.Effect;
import mindustry.entities.TargetPriority;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Sector;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class StrikeTurret extends Block{
    /** Индекс = тип снаряда: 0 взрывчатка, 1 пиротит, 2 кремний (кассетный). */
    public static final Item[] ammo = {Items.blastCompound, Items.pyratite, Items.silicon};

    public float reload = 480f;

    public StrikeTurret(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        group = BlockGroup.turrets;
        priority = TargetPriority.turret;
        flags = EnumSet.of(BlockFlag.turret);
        consumePower(8f);
        buildType = StrikeBuild::new;

        config(int[].class, (StrikeBuild b, int[] v) -> {
            b.target = v[0];
            b.fx = v[1];
            b.fy = v[2];
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("ammo", (StrikeBuild e) -> new Bar("stat.ammo", Pal.ammo, () -> e.items.total() / (float)(itemCapacity * ammo.length)));
        addBar("reload", (StrikeBuild e) -> new Bar("stat.reload", Pal.accent, () -> Mathf.clamp(e.charge / reload)));
    }

    public static int typeOf(Item item){
        for(int i = 0; i < ammo.length; i++) if(ammo[i] == item) return i;
        return -1;
    }

    public class StrikeBuild extends Building{
        public int target = -1, fx = 5000, fy = 5000;
        public float charge;
        public Item last;

        @Override
        public boolean acceptItem(Building source, Item item){
            return typeOf(item) >= 0 && items.get(item) < itemCapacity;
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return typeOf(item) < 0 ? 0 : Math.min(itemCapacity - items.get(item), amount);
        }

        @Override
        public void handleItem(Building source, Item item){
            super.handleItem(source, item);
            last = item;
        }

        Item pickAmmo(){
            if(last != null && items.has(last)) return last;
            for(Item i : ammo) if(items.has(i)) return i;
            return null;
        }

        @Override
        public void updateTile(){
            Item a = pickAmmo();
            // Мир загружен только пока игрок в этом секторе, поэтому стреляем только тогда.
            if(target < 0 || a == null || !state.isCampaign() || state.rules.sector == null){
                charge = Math.min(charge, reload);
                return;
            }
            charge += delta() * efficiency;
            if(charge >= reload){
                charge = 0f;
                fire(a);
            }
        }

        void fire(Item a){
            Sector home = state.rules.sector;
            if(target >= home.planet.sectors.size) return;
            Sector t = home.planet.sectors.get(target);
            Strikes.queue(t, typeOf(a), fx, fy);
            items.remove(a, 1);
            Fx.shootBig.at(x, y, 90f);
            Snd.play(Snd.artillery(), x, y, Mathf.random(0.8f, 0.95f));
            Effect.shake(4f, 6f, x, y);
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.map, Styles.cleari, () -> {
                control.input.config.hideConfig();
                StrikeUI.open(this);
            }).size(48f);
            table.button(Icon.cancel, Styles.cleari, () -> configure(new int[]{-1, 0, 0})).size(48f);
        }

        @Override
        public Object config(){
            return new int[]{target, fx, fy};
        }

        @Override
        public void write(Writes w){
            super.write(w);
            w.i(target);
            w.i(fx);
            w.i(fy);
            w.f(charge);
        }

        @Override
        public void read(Reads r, byte revision){
            super.read(r, revision);
            target = r.i();
            fx = r.i();
            fy = r.i();
            charge = r.f();
        }
    }
}
