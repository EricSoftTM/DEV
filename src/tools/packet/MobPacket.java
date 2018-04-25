/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.packet;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.SendPacketOpcode;
import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.MapleMap;
import server.maps.MapleNodes.MapleNodeInfo;
import server.movement.LifeMovementFragment;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

public class MobPacket {

    public static byte[] damageMonster(final int oid, final int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        if (damage > Integer.MAX_VALUE || damage < 0) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt(damage);
        }

        return mplew.getPacket();
    }

    public static byte[] damageFriendlyMob(final MapleMonster mob, int damage, final boolean display) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(display ? 1 : 2); //false for when shammos changes map!
        if (damage > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt(damage);
        }
        if (mob.getHp() > Integer.MAX_VALUE) {
            mplew.writeInt((int)((mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) mob.getMobMaxHp());
        }
        return mplew.getPacket();
    }

    public static byte[] killMonster(final int oid, final int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); // 0 = dissapear, 1 = fade out, 2+ = special
        if (animation == 4) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] killAswanMonster(final int oid, final int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); // 0 = dissapear, 1 = fade out, 2+ = special
        if (animation == 4) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }
    
    public static byte[] suckMonster(final int oid, final int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(4);
        mplew.writeInt(chr);

        return mplew.getPacket();
    }

    public static byte[] healMonster(final int oid, final int heal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(-heal);
        mplew.writeZeroBytes(100);
        return mplew.getPacket();
    }

    public static byte[] MobToMobDamage(final int oid, final int dmg, final int mobid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOB_TO_MOB_DAMAGE.getValue());
        mplew.writeInt(oid);
        mplew.write(0); // looks like the effect, must be > -2
        mplew.writeInt(dmg);
        mplew.writeInt(mobid);
        mplew.write(1); // ?

        return mplew.getPacket();
    }

    public static byte[] getMobSkillEffect(final int oid, final int skillid, final int cid, final int skilllevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT_MOB.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(skillid); // 3110001, 3210001, 13110009, 2210000
        mplew.writeInt(cid);
        mplew.writeShort(skilllevel);

        return mplew.getPacket();
    }

    public static byte[] getMobCoolEffect(final int oid, final int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ITEM_EFFECT_MOB.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid); // 2022588

        return mplew.getPacket();
    }

    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);

        return mplew.getPacket();
    }

    public static byte[] showCygnusAttack(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CYGNUS_ATTACK.getValue());
        mplew.writeInt(oid); // mob must be 8850011

        return mplew.getPacket();
    }

    public static byte[] showMonsterResist(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_RESIST.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(0);
        mplew.writeShort(1); // resist >0
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] showBossHP(final MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(mob.getId() == 9400589 ? 9300184 : mob.getId()); //hack: MV cant have boss hp bar
        if (mob.getHp() > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) mob.getMobMaxHp());
        }
        mplew.write(mob.getStats().getTagColor());
        mplew.write(mob.getStats().getTagBgColor());
mplew.writeZeroBytes(30);
        return mplew.getPacket();
    }

    public static byte[] showBossHP(final int monsterId, final long currentHp, final long maxHp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(monsterId); //has no image
        if (currentHp > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) currentHp / maxHp) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) (currentHp <= 0 ? -1 : currentHp));
        }
        if (maxHp > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) maxHp);
        }
        mplew.write(6);
        mplew.write(5);

        //colour legend: (applies to both colours)
        //1 = red, 2 = dark blue, 3 = light green, 4 = dark green, 5 = black, 6 = light blue, 7 = purple
mplew.writeZeroBytes(30);
        return mplew.getPacket();
    }

    public static byte[] moveMonster(boolean useskill, int skill, int unk, int oid, Point startPos, List<LifeMovementFragment> moves) {
        return moveMonster(useskill, skill, unk, oid, startPos, moves, null, null);
    }

    public static byte[] moveMonster(boolean useskill, int skill, int unk, int oid, Point startPos, List<LifeMovementFragment> moves, final List<Integer> unk2, final List<Pair<Integer, Integer>> unk3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(useskill ? 1 : 0);
        mplew.write(skill);
        mplew.writeInt(unk);
        mplew.write(unk3 == null ? 0 : unk3.size()); // For each, 2 short
        if (unk3 != null) {
            for (final Pair<Integer, Integer> i : unk3) {
                mplew.writeShort(i.left);
                mplew.writeShort(i.right);
            }
        }
        mplew.write(unk2 == null ? 0 : unk2.size()); // For each, 1 short
        if (unk2 != null) {
            for (final Integer i : unk2) {
                mplew.writeShort(i);
            }
        }
        mplew.writePos(startPos);
        mplew.writeShort(8);
        mplew.writeShort(1);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }
    
    
        public static byte[] movePokemon(int oid, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0); // For each, 2 short
        mplew.write(0); // For each, 1 short
        mplew.writePos(startPos);
        mplew.writeShort(8);
        mplew.writeShort(1);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }
    

/*     */   public static byte[] spawnMonster(MapleMonster life, int spawnType, int link) {
/* 266 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 268 */     mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
/* 269 */     mplew.writeInt(life.getObjectId());
/* 270 */     mplew.write(1);
/* 271 */     mplew.writeInt(life.getId());
/* 272 */     addMonsterStatus(mplew, life);
/* 273 */     mplew.writePos(life.getTruePosition());
/* 274 */     mplew.write(life.getStance());
/* 275 */     mplew.writeShort(0);
/* 276 */     mplew.writeShort(life.getFh());
/* 277 */     mplew.write(spawnType);
/* 278 */     if ((spawnType == -3) || (spawnType >= 0)) {
/* 279 */       mplew.writeInt(link);
/*     */     }
/* 281 */     mplew.write(life.getCarnivalTeam());
/* 282 */     mplew.writeInt(63000);
/* 283 */     mplew.writeInt(0);
/* 284 */     mplew.writeInt(0);
/* 285 */     mplew.write(-1);
/* 286 */     return mplew.getPacket();
/*     */   }
/*     */ 

public static byte[] spawnAswanMonster(MapleMonster life, int spawnType, int link) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ASWAN_SPAWN_MONSTER.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.write(1);
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);
        mplew.writeShort(life.getFh());
        mplew.write(spawnType);
        if ((spawnType == -3) || (spawnType >= 0)) {
            mplew.writeInt(link);
        }
        mplew.write(life.getCarnivalTeam());
        mplew.writeInt(63000);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(-1);
//        System.out.println("spawnAswanMonster: " + mplew.getPacket());
        return mplew.getPacket();
    }


    public static void addMonsterStatus(MaplePacketLittleEndianWriter mplew, MapleMonster life) {
        if (life.getStati().size() <= 1) {
            life.addEmpty(); //not done yet lulz ok so we add it now for the lulz
        }
        mplew.write(life.getChangedStats() != null ? 1 : 0);
        if (life.getChangedStats() != null) {
            mplew.writeInt(life.getChangedStats().hp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) life.getChangedStats().hp);
            mplew.writeInt(life.getChangedStats().mp);
            mplew.writeInt(life.getChangedStats().exp);
            mplew.writeInt(life.getChangedStats().watk);
            mplew.writeInt(life.getChangedStats().matk);
            mplew.writeInt(life.getChangedStats().PDRate);
            mplew.writeInt(life.getChangedStats().MDRate);
            mplew.writeInt(life.getChangedStats().acc);
            mplew.writeInt(life.getChangedStats().eva);
            mplew.writeInt(life.getChangedStats().pushed);
            mplew.writeInt(life.getChangedStats().level);
        }
        final boolean ignore_imm = life.getStati().containsKey(MonsterStatus.WEAPON_DAMAGE_REFLECT) || life.getStati().containsKey(MonsterStatus.MAGIC_DAMAGE_REFLECT);
        Collection<MonsterStatusEffect> buffs = life.getStati().values();
        getLongMask_NoRef(mplew, buffs, ignore_imm);
        for (MonsterStatusEffect buff : buffs) {
            if (buff != null && buff.getStati() != MonsterStatus.WEAPON_DAMAGE_REFLECT && buff.getStati() != MonsterStatus.MAGIC_DAMAGE_REFLECT && (!ignore_imm || (buff.getStati() != MonsterStatus.WEAPON_IMMUNITY && buff.getStati() != MonsterStatus.MAGIC_IMMUNITY && buff.getStati() != MonsterStatus.DAMAGE_IMMUNITY))) {
                if (buff.getStati() != MonsterStatus.SUMMON && buff.getStati() != MonsterStatus.EMPTY_3) {
                    if (buff.getStati() == MonsterStatus.EMPTY_1 || buff.getStati() == MonsterStatus.EMPTY_2 || buff.getStati() == MonsterStatus.EMPTY_3 || buff.getStati() == MonsterStatus.EMPTY_4 || buff.getStati() == MonsterStatus.EMPTY_5 || buff.getStati() == MonsterStatus.EMPTY_6) {
                        mplew.writeShort(Integer.valueOf((int) System.currentTimeMillis()).shortValue());
                        mplew.writeShort(0);
/* 316 */           } else if (buff.getStati() == MonsterStatus.EMPTY_7) {
/* 317 */             mplew.write(0);
/*     */           } else {
/* 319 */             mplew.writeInt(buff.getX().intValue());
/*     */           }
                    if (buff.getMobSkill() != null) {
                        mplew.writeShort(buff.getMobSkill().getSkillId());
                        mplew.writeShort(buff.getMobSkill().getSkillLevel());
                    } else if (buff.getSkill() > 0) {
                        mplew.writeInt(buff.getSkill());
                    }
                }
                if (buff.getStati() != MonsterStatus.EMPTY_7) {
                mplew.writeShort(buff.getStati() == MonsterStatus.HYPNOTIZE ? 40 : (buff.getStati().isEmpty() ? 0 : 1));
                if (buff.getStati() == MonsterStatus.EMPTY_1 || buff.getStati() == MonsterStatus.EMPTY_3) {
                    mplew.writeShort(0);
                } else if (buff.getStati() == MonsterStatus.EMPTY_4 || buff.getStati() == MonsterStatus.EMPTY_5) {
                    mplew.writeInt(0);
                }
            }
            }
        }
    }

/*     */   public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro)
/*     */   {
/* 341 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 343 */     mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
/* 344 */     mplew.write(aggro ? 2 : 1);
/* 345 */     mplew.writeInt(life.getObjectId());
/* 346 */     mplew.write(1);
/* 347 */     mplew.writeInt(life.getId());
/* 348 */     addMonsterStatus(mplew, life);
/*     */ 
/* 350 */     mplew.writePos(life.getTruePosition());
/* 351 */     mplew.write(life.getStance());
/* 352 */     mplew.writeShort(0);
/* 353 */     mplew.writeShort(life.getFh());
/* 354 */     mplew.write(newSpawn ? -2 : life.isFake() ? -4 : -1);
/* 355 */     mplew.write(life.getCarnivalTeam());
/* 356 */     mplew.writeInt(63000);
/* 357 */     mplew.writeInt(0);
/* 358 */     mplew.writeInt(0);
/* 359 */     mplew.write(-1);
/*     */ 
/* 361 */     return mplew.getPacket();
/*     */   }

 public static byte[] controlAswanMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ASWAN_SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(aggro ? 2 : 1);
        mplew.writeInt(life.getObjectId());
        mplew.write(1);
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);

        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);
        mplew.writeShort(life.getFh());
        mplew.write(newSpawn ? -2 : life.isFake() ? -4 : -1);
        mplew.write(life.getCarnivalTeam());
        mplew.writeInt(63000);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(-1);

       // System.out.println("controlAswanMonster: " + mplew.getPacket());
        return mplew.getPacket();
    }


    public static byte[] stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }
    
    public static byte[] stopControllingAswanMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ASWAN_SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        //System.out.println("stopControllingAswanMonster: " + mplew.getPacket());
        return mplew.getPacket();
    }

    public static byte[] makeAswanMonsterInvisible(MapleMonster life) {
        //System.out.println("accessing makeAswanMonsterInvisible");
        return spawnAswanMonster(life, -4, 0);
    }


    public static byte[] makeMonsterReal(MapleMonster life) {
        return spawnMonster(life, -1, 0);
    }
    
    public static byte[] makeAswanMonsterReal(MapleMonster life) {
//        System.out.println("accessing makeAswanMonsterReal");
        return spawnAswanMonster(life, -1, 0);
    }


    public static byte[] makeMonsterFake(MapleMonster life) {
        return spawnMonster(life, -4, 0);
    }

    public static byte[] makeMonsterEffect(MapleMonster life, int effect) {
        return spawnMonster(life, effect, 0);
    }
    
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }
    
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills ? 1 : 0);
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        mplew.writeInt(0);

        return mplew.getPacket();
    }
    
    public static Object movePokemon(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
         // mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
                
        mplew.writeInt(objectid);
        mplew.write(moveid);
        mplew.write(useSkills ? 1 : 0);
        mplew.writeInt(currentMp);
        
        // mplew.write(skillId);
        // mplew.write(skillLevel);
        // mplew.writeInt(0);
        
        mplew.write(0); // For each, 2 short
        mplew.write(0); // For each, 1 short
        mplew.writePos(startPos);
        mplew.writeShort(8);
        mplew.writeShort(1);
        PacketHelper.serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    private static void getLongMask_NoRef(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> ss, boolean ignore_imm) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (MonsterStatusEffect statup : ss) {
            if (statup != null && statup.getStati() != MonsterStatus.WEAPON_DAMAGE_REFLECT && statup.getStati() != MonsterStatus.MAGIC_DAMAGE_REFLECT && (!ignore_imm || (statup.getStati() != MonsterStatus.WEAPON_IMMUNITY && statup.getStati() != MonsterStatus.MAGIC_IMMUNITY && statup.getStati() != MonsterStatus.DAMAGE_IMMUNITY))) {
                mask[statup.getStati().getPosition() - 1] |= statup.getStati().getValue();
            }
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

 /*     */   public static byte[] applyMonsterStatus(int oid, MonsterStatus mse, int x, MobSkill skil)
/*     */   {
/* 408 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 410 */     mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
/* 411 */     mplew.writeInt(oid);
/* 412 */     PacketHelper.writeSingleMask(mplew, mse);
/*     */ 
/* 414 */     mplew.writeInt(x);
/* 415 */     mplew.writeShort(skil.getSkillId());
/* 416 */     mplew.writeShort(skil.getSkillLevel());
/* 417 */     mplew.writeShort(mse.isEmpty() ? 1 : 0);
/*     */ 
/* 419 */     mplew.writeShort(0);
/* 420 */     mplew.write(1);
/* 421 */     mplew.write(1);
/*     */ 
/* 423 */     return mplew.getPacket();
/*     */   }
/*     */ 
/*     */   public static byte[] applyMonsterStatus(MapleMonster mons, MonsterStatusEffect ms)
/*     */   {
/* 430 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 432 */     mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
/* 433 */     mplew.writeInt(mons.getObjectId());
/* 434 */     PacketHelper.writeSingleMask(mplew, ms.getStati());
/*     */ 
/* 436 */     mplew.writeInt(ms.getX().intValue());
/* 437 */     if (ms.isMonsterSkill()) {
/* 438 */       mplew.writeShort(ms.getMobSkill().getSkillId());
/* 439 */       mplew.writeShort(ms.getMobSkill().getSkillLevel());
/* 440 */     } else if (ms.getSkill() > 0) {
/* 441 */       mplew.writeInt(ms.getSkill());
/*     */     }
/* 443 */     mplew.writeShort(ms.getStati().isEmpty() ? 1 : 0);
/*     */ 
/* 445 */     mplew.writeShort(0);
/* 446 */     mplew.write(1);
/* 447 */     mplew.write(1);
/*     */ 
/* 449 */     return mplew.getPacket();
/*     */   }
/*     */ 
/*     */   public static byte[] applyMonsterStatus(MapleMonster mons, List<MonsterStatusEffect> mse) {
/* 453 */     if ((mse.size() <= 0) || (mse.get(0) == null)) {
/* 454 */       return CWvsContext.enableActions();
/*     */     }
/* 456 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 458 */     mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
/* 459 */     mplew.writeInt(mons.getObjectId());
/* 460 */     MonsterStatusEffect ms = (MonsterStatusEffect)mse.get(0);
/* 461 */     if (ms.getStati() == MonsterStatus.POISON) {
/* 462 */       PacketHelper.writeSingleMask(mplew, MonsterStatus.EMPTY);
/* 463 */       mplew.write(mse.size());
/* 464 */       for (MonsterStatusEffect m : mse) {
/* 465 */         mplew.writeInt(m.getFromID());
/* 466 */         if (m.isMonsterSkill()) {
/* 467 */           mplew.writeShort(m.getMobSkill().getSkillId());
/* 468 */           mplew.writeShort(m.getMobSkill().getSkillLevel());
/* 469 */         } else if (m.getSkill() > 0) {
/* 470 */           mplew.writeInt(m.getSkill());
/*     */         }
/* 472 */         mplew.writeInt(m.getX().intValue());
/* 473 */         mplew.writeInt(1000);
/* 474 */         mplew.writeInt(0);
/* 475 */         mplew.writeInt(5);
/* 476 */         mplew.writeInt(0);
/*     */       }
/* 478 */       mplew.writeShort(300);
/* 479 */       mplew.write(1);
/* 480 */       mplew.write(1);
/*     */     } else {
/* 482 */       PacketHelper.writeSingleMask(mplew, ms.getStati());
/*     */ 
/* 484 */       mplew.writeInt(ms.getX().intValue());
/* 485 */       if (ms.isMonsterSkill()) {
/* 486 */         mplew.writeShort(ms.getMobSkill().getSkillId());
/* 487 */         mplew.writeShort(ms.getMobSkill().getSkillLevel());
/* 488 */       } else if (ms.getSkill() > 0) {
/* 489 */         mplew.writeInt(ms.getSkill());
/*     */       }
/* 491 */       mplew.writeShort(0);
/*     */ 
/* 493 */       mplew.writeShort(0);
/* 494 */       mplew.write(1);
/* 495 */       mplew.write(1);
/*     */     }
/*     */ 
/* 498 */     return mplew.getPacket();
/*     */   }
/*     */ 
/*     */   public static byte[] applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stati, List<Integer> reflection, MobSkill skil) {
/* 502 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 504 */     mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
/* 505 */     mplew.writeInt(oid);
/* 506 */     PacketHelper.writeMask(mplew, stati.keySet());
/*     */ 
/* 508 */     for (Map.Entry mse : stati.entrySet()) {
/* 509 */       mplew.writeInt(((Integer)mse.getValue()).intValue());
/* 510 */       mplew.writeShort(skil.getSkillId());
/* 511 */       mplew.writeShort(skil.getSkillLevel());
/* 512 */       mplew.writeShort(0);
/*     */     }
/*     */ 
/* 515 */     for (Integer ref : reflection) {
/* 516 */       mplew.writeInt(ref.intValue());
/*     */     }
/* 518 */     mplew.writeLong(0L);
/* 519 */     mplew.writeShort(0);
/*     */ 
/* 521 */     int size = stati.size();
/* 522 */     if (reflection.size() > 0) {
/* 523 */       size /= 2;
/*     */     }
/* 525 */     mplew.write(size);
/* 526 */     mplew.write(1);
/*     */ 
/* 528 */     return mplew.getPacket();
/*     */   }
/*     */ 

    public static byte[] cancelMonsterStatus(int oid, MonsterStatus stat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        PacketHelper.writeSingleMask(mplew, stat);
        mplew.write(1); // reflector is 3~!??
        mplew.write(2); // ? v97

        return mplew.getPacket();
    }

    public static byte[] cancelPoison(int oid, MonsterStatusEffect m) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        PacketHelper.writeSingleMask(mplew, MonsterStatus.EMPTY);
        mplew.writeInt(0);
        mplew.writeInt(1); //size probably
        mplew.writeInt(m.getFromID()); //character ID
        if (m.isMonsterSkill()) {
            mplew.writeShort(m.getMobSkill().getSkillId());
            mplew.writeShort(m.getMobSkill().getSkillLevel());
        } else if (m.getSkill() > 0) {
            mplew.writeInt(m.getSkill());
        }
        mplew.write(3); // ? v97

        return mplew.getPacket();
    }

    public static byte[] talkMonster(int oid, int itemId, int seconds, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TALK_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(seconds > 0 ? (seconds * 1000) : 500);
        mplew.writeInt(itemId);
        mplew.write(itemId <= 3 ? 0 : 1);
        mplew.write(msg == null || msg.length() <= 0 ? 0 : 1);
        if (msg != null && msg.length() > 0) {
            mplew.writeMapleAsciiString(msg);
        }
        mplew.writeInt(1); // unk
        return mplew.getPacket();
    }

    public static byte[] removeTalkMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_TALK_MONSTER.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static byte[] getNodeProperties(final MapleMonster objectid, final MapleMap map) {
        if (objectid.getNodePacket() != null) {
            return objectid.getNodePacket();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_PROPERTIES.getValue());
        mplew.writeInt(objectid.getObjectId());
        mplew.writeInt(map.getNodes().size());
        mplew.writeInt(objectid.getPosition().x);
        mplew.writeInt(objectid.getPosition().y);
        for (MapleNodeInfo mni : map.getNodes()) {
            mplew.writeInt(mni.x);
            mplew.writeInt(mni.y);
            mplew.writeInt(mni.attr);
            if (mni.attr == 2) { //msg
                mplew.writeInt(500); //? talkMonster
            }
        }
        mplew.writeInt(0);
        mplew.write(0); // tickcount, extra 1 int
        mplew.write(0);
        objectid.setNodePacket(mplew.getPacket());
        return objectid.getNodePacket();
    }

    public static byte[] showMagnet(int mobid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success ? 1 : 0);
        mplew.write(0); // times, 0 = once, > 0 = twice

        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(mobid);
        mplew.writeInt(itemid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static byte[] showBossHPPlayer(int monsterId, long currentHp, long maxHp, byte tagColor, byte tagbgcolor) {
/* 208 */     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
/*     */ 
/* 210 */     mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
/* 211 */     mplew.write(5);
/* 212 */     mplew.writeInt(monsterId);
/* 213 */     if (currentHp > 2147483647L)
/* 214 */       mplew.writeInt((int)(currentHp / maxHp * 2147483647.0D));
/*     */     else {
/* 216 */       mplew.writeInt((int)(currentHp <= 0L ? -1L : currentHp));
/*     */     }
/* 218 */     if (maxHp > 2147483647L)
/* 219 */       mplew.writeInt(2147483647);
/*     */     else {
/* 221 */       mplew.writeInt((int)maxHp);
/*     */     }
/* 223 */     mplew.write(tagColor);
/* 224 */     mplew.write(tagbgcolor);
/*     */ 
/* 229 */     return mplew.getPacket();
/*     */   }
}
