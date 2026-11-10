package sectorstrike;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Sector;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

public class StrikeUI{

    static float boxSize(float k){
        return Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / Scl.scl(1f) * k;
    }

    static String describe(Sector s){
        String st = s.hasEnemyBase() ? Core.bundle.get("ss.enemy") : s.hasBase() ? Core.bundle.get("ss.base") : Core.bundle.get("ss.empty");
        return s.name() + " (" + st + ")";
    }

    /** Шаг 1: карта прилегающих секторов. */
    public static void open(StrikeTurret.StrikeBuild b){
        Sector home = state.rules.sector;
        if(home == null || !state.isCampaign()){
            ui.showInfoToast(Core.bundle.get("ss.needcampaign"), 3f);
            return;
        }

        BaseDialog d = new BaseDialog("@ss.neighbors");
        NearMap map = new NearMap(home, s -> pickPoint(b, s, d));
        d.cont.add(map).size(boxSize(0.6f)).row();
        d.cont.label(() -> map.hover == null ? Core.bundle.get("ss.hover") : describe(map.hover)).pad(6f).row();
        d.addCloseButton();
        d.show();
    }

    /** Шаг 2: выбор точки в секторе. */
    static void pickPoint(StrikeTurret.StrikeBuild b, Sector sec, BaseDialog parent){
        BaseDialog d = new BaseDialog(Core.bundle.format("ss.point", sec.name()));
        PointMap map = new PointMap();
        d.cont.add(map).size(boxSize(0.55f)).row();
        d.cont.add(Core.bundle.get("ss.hint")).pad(6f).row();
        d.buttons.defaults().size(180f, 64f);
        d.buttons.button("@back", Icon.left, d::hide);
        d.buttons.button("@ss.confirm", Icon.ok, () -> {
            if(!map.set) return;
            b.configure(new int[]{sec.id, (int)(map.px * 10000f), (int)(map.py * 10000f)});
            d.hide();
            parent.hide();
        });
        d.show();
    }

    static class NearMap extends Element{
        final Sector home;
        final Seq<Sector> near = new Seq<>();
        final Seq<Vec2> pos = new Seq<>();
        final Cons<Sector> cb;
        Sector hover;

        NearMap(Sector home, Cons<Sector> cb){
            this.home = home;
            this.cb = cb;

            Vec3 c = home.tile.v;
            Vec3 east = new Vec3(0f, 1f, 0f).crs(c).nor();
            if(east.len() < 0.01f) east.set(1f, 0f, 0f);
            Vec3 north = new Vec3(c).crs(east).nor();
            float max = 0.0001f;
            for(var t : home.tile.tiles){
                near.add(home.planet.getSector(t));
                Vec3 dv = new Vec3(t.v).sub(c);
                Vec2 p = new Vec2(dv.dot(east), dv.dot(north));
                pos.add(p);
                max = Math.max(max, p.len());
            }
            for(Vec2 p : pos) p.scl(1f / max);

            addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    hover = find(x, y);
                    if(hover != null) cb.get(hover);
                }

                @Override
                public boolean mouseMoved(InputEvent event, float x, float y){
                    hover = find(x, y);
                    return false;
                }
            });
        }

        float radius(){ return Math.min(width, height) * 0.34f; }

        Sector find(float x, float y){
            float R = radius(), hr = R * 0.46f;
            for(int i = 0; i < near.size; i++){
                Vec2 p = pos.get(i);
                if(Mathf.dst(x, y, width / 2f + p.x * R, height / 2f + p.y * R) < hr) return near.get(i);
            }
            return null;
        }

        @Override
        public void draw(){
            float cx = x + width / 2f, cy = y + height / 2f, R = radius(), hr = R * 0.46f;

            Draw.color(Pal.accent);
            Fill.poly(cx, cy, 6, hr * 0.9f);
            Draw.color(Color.black);
            Fill.poly(cx, cy, 6, hr * 0.7f);

            for(int i = 0; i < near.size; i++){
                Sector s = near.get(i);
                Vec2 p = pos.get(i);
                float px = cx + p.x * R, py = cy + p.y * R;
                Color c = s.hasEnemyBase() ? Color.scarlet : s.hasBase() ? Pal.heal : Pal.lightishGray;
                Draw.color(c, s == hover ? 1f : 0.55f);
                Fill.poly(px, py, 6, hr * 0.9f);
                Draw.color(Color.black, 0.6f);
                Fill.poly(px, py, 6, hr * 0.7f);
                Draw.color(c, s == hover ? 0.9f : 0.35f);
                Fill.poly(px, py, 6, hr * 0.6f);
            }
            Draw.reset();
        }
    }

    static class PointMap extends Element{
        float px = 0.5f, py = 0.5f;
        boolean set;
        static final float assumedSize = 500f;

        PointMap(){
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent e, float x, float y, int pointer, KeyCode button){
                    upd(x, y);
                    return true;
                }

                @Override
                public void touchDragged(InputEvent e, float x, float y, int pointer){
                    upd(x, y);
                }
            });
        }

        void upd(float lx, float ly){
            px = Mathf.clamp(lx / width);
            py = Mathf.clamp(ly / height);
            set = true;
        }

        @Override
        public void draw(){
            Draw.color(Color.black, 0.85f);
            Fill.rect(x + width / 2f, y + height / 2f, width, height);

            Draw.color(Pal.gray);
            Lines.stroke(1f);
            for(int i = 1; i < 10; i++){
                Lines.line(x + width * i / 10f, y, x + width * i / 10f, y + height);
                Lines.line(x, y + height * i / 10f, x + width, y + height * i / 10f);
            }
            Draw.color(Pal.accent);
            Lines.stroke(2f);
            Lines.rect(x, y, width, height);

            if(set){
                float cx = x + px * width, cy = y + py * height;
                float sw = 70f / assumedSize * width, sh = 70f / assumedSize * height;
                Draw.color(Color.scarlet);
                Lines.stroke(2f);
                Lines.rect(cx - sw / 2f, cy - sh / 2f, sw, sh);
                Lines.line(cx - 10f, cy, cx + 10f, cy);
                Lines.line(cx, cy - 10f, cx, cy + 10f);
                Lines.circle(cx, cy, 6f);
            }
            Draw.reset();
        }
    }
}
