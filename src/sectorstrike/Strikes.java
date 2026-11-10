package sectorstrike;

import arc.Core;
import arc.audio.Sound;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Sector;
import mindustry.world.Tile;

import static mindustry.Vars.*;

/** Очередь ударов по незагруженным секторам + применение при заходе игрока. */
public class Strikes{
    static final int maxPending = 120;
    static final float scatter = 35f; // 70x70 блоков

    static final BulletType sub = new BasicBulletType(3.2f, 0f){{
        width = 9f;
        height = 12f;
        lifetime = 90f;
        homingPower = 0.12f;
        homingRange = 260f;
        splashDamage = 380f;
        splashDamageRadius = 40f;
        backColor = Pal.bulletYellowBack;
        frontColor = Pal.bulletYellow;
        trailColor = Pal.bulletYellowBack;
        trailLength = 8;
        trailWidth = 2f;
        hitEffect = Fx.massiveExplosion;
        despawnEffect = Fx.massiveExplosion;
        Sound hs = Snd.explosion();
        if(hs != null) hitSound = hs;
    }};

    static String key(Sector s){
        return "ss-" + s.planet.name + "-" + s.id;
    }

    public static void queue(Sector s, int type, int fx, int fy){
        String k = key(s);
        String cur = Core.settings.getString(k, "");
        if(!cur.isEmpty() && cur.split(";").length >= maxPending){
            cur = cur.substring(cur.indexOf(';') + 1);
        }
        String e = type + "," + fx + "," + fy;
        Core.settings.put(k, cur.isEmpty() ? e : cur + ";" + e);
    }

    public static void onWorldLoad(){
        if(!state.isCampaign() || state.rules.sector == null) return;
        Sector sec = state.rules.sector;
        String k = key(sec);
        String all = Core.settings.getString(k, "");
        if(all.isEmpty()) return;
        Core.settings.remove(k);

        Team team = state.rules.defaultTeam;
        int id = sec.id;
        String[] parts = all.split(";");
        for(int i = 0; i < parts.length; i++){
            String[] a = parts[i].split(",");
            if(a.length < 3) continue;
            int type = Integer.parseInt(a[0]), fx = Integer.parseInt(a[1]), fy = Integer.parseInt(a[2]);
            Time.run(240f + i * 45f + Mathf.random(0f, 30f), () -> impact(id, team, type, fx, fy));
        }
    }

    static void impact(int sectorId, Team team, int type, int fx, int fy){
        if(!state.isGame() || state.rules.sector == null || state.rules.sector.id != sectorId) return;

        float w = world.width() * tilesize, h = world.height() * tilesize;
        float x = Mathf.clamp(fx / 10000f * w + Mathf.range(scatter * tilesize), 0f, w);
        float y = Mathf.clamp(fy / 10000f * h + Mathf.range(scatter * tilesize), 0f, h);

        switch(type){
            case 0 -> { // взрывчатая смесь: как ториевый реактор (радиус 19, урон 5000)
                Fx.reactorExplosion.at(x, y);
                Snd.play(Snd.explosion(), x, y, 0.6f);
                Effect.shake(40f, 60f, x, y);
                Damage.damage(team, x, y, 19f * tilesize, 5000f, true);
            }
            case 1 -> { // пиротит: меньше урона, но много огня
                float r = 12f * tilesize;
                Fx.massiveExplosion.at(x, y);
                Snd.play(Snd.explosion(), x, y, 0.8f);
                Effect.shake(20f, 40f, x, y);
                Damage.damage(team, x, y, r, 2600f, true);
                for(int i = 0; i < 45; i++){
                    Tile t = world.tileWorld(x + Mathf.range(r), y + Mathf.range(r));
                    if(t != null) Fires.create(t);
                }
            }
            case 2 -> { // кремний: кассета с самонаводящимися боеприпасами
                for(int i = 0; i < 16; i++){
                    Time.run(i * 5f, () -> {
                        if(!state.isGame()) return;
                        sub.create(null, team, x + Mathf.range(25f * tilesize), y + Mathf.range(25f * tilesize), Mathf.random(360f));
                    });
                }
            }
            default -> {}
        }
    }
}
