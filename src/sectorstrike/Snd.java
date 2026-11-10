package sectorstrike;

import arc.audio.Sound;
import mindustry.gen.Sounds;

/** Безопасный доступ к звукам: в build 154 многие звуки переименованы, поэтому ищем по списку имён. */
public class Snd{
    public static Sound explosion(){ return get("explosion", "explosionBig", "bigExplosion", "explode", "explosionLarge"); }
    public static Sound artillery(){ return get("artillery", "shootBig", "largeCannon", "cannon", "shootArtillery", "artilleryBig", "shootLarge"); }

    static Sound get(String... names){
        for(String n : names){
            try{
                Object o = Sounds.class.getField(n).get(null);
                if(o instanceof Sound s) return s;
            }catch(Throwable ignored){}
        }
        return null;
    }

    public static void play(Sound s, float x, float y, float volume){
        if(s != null) s.at(x, y, volume);
    }
}
