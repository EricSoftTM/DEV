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

import client.MapleTrait.MapleTraitType;
import client.*;
import client.inventory.*;
import constants.GameConstants;
import handling.Buffstat;
import handling.world.MapleCharacterLook;
import java.util.Map.Entry;
import java.util.*;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopItem;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.AbstractPlayerStore;
import server.shops.IMaplePlayerShop;
import tools.BitTools;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

public class PacketHelper {

    public final static long FT_UT_OFFSET = 116444592000000000L; // EDT
    public final static long MAX_TIME = 150842304000000000L; //00 80 05 BB 46 E6 17 02
    public final static long ZERO_TIME = 94354848000000000L; //00 40 E0 FD 3B 37 4F 01
    public final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

    public static long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    public static long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        }
        return ((realTimestamp * 10000) + FT_UT_OFFSET);
    }

    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (SimpleTimeZone.getDefault().inDaylightTime(new Date())) {
            timeStampinMillis -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1000 / 60) * 600000000;
        } else {
            time = timeStampinMillis * 10000;
        }
        return time + FT_UT_OFFSET;
    }

    public static void addQuestInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final boolean idk = true;

        // 0x2000
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.write(idk ? 1 : 0); // boolean
        if (idk) {
            mplew.writeShort(started.size());
            for (final MapleQuestStatus q : started) {
                mplew.writeShort(q.getQuest().getId());
                if (q.hasMobKills()) {
                    final StringBuilder sb = new StringBuilder();
                    for (final int kills : q.getMobKills().values()) {
                        sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                    }
                    mplew.writeMapleAsciiString(sb.toString());
                } else {
                    mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
                }
            }

        } else {
            mplew.writeShort(0); // size, one short per size
        }
        mplew.writeShort(0); // size, two strings per size

        // 0x4000
        mplew.write(idk ? 1 : 0); //dunno
        if (idk) {
            final List<MapleQuestStatus> completed = chr.getCompletedQuests();
            mplew.writeShort(completed.size());
            for (final MapleQuestStatus q : completed) {
                mplew.writeShort(q.getQuest().getId());
                mplew.writeLong(getTime(q.getCompletionTime()));
            }
        } else {
            mplew.writeShort(0); // size, one short per size
        }
    }

    public static void addSkillInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) { // 0x100
        final Map<Skill, SkillEntry> skills = chr.getSkills();
        boolean useOld = skills.size() < 500;
        mplew.write(useOld ? 1 : 0); // To handle the old skill system or something? 
        if (useOld) {
            mplew.writeShort(skills.size());
            for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                mplew.writeInt(skill.getKey().getId());
                mplew.writeInt(skill.getValue().skillevel);
                addExpirationTime(mplew, skill.getValue().expiration);

                if (skill.getKey().isFourthJob()) {
                    mplew.writeInt(skill.getValue().masterlevel);
                }
            }
        } else {
            final Map<Integer, Integer> skillsWithoutMax = new LinkedHashMap<>();
            final Map<Integer, Long> skillsWithExpiration = new LinkedHashMap<>();
            final Map<Integer, Integer> skillsWithMax = new LinkedHashMap<>();

            // Fill in these maps
            for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                skillsWithoutMax.put(skill.getKey().getId(), skill.getValue().skillevel);
                if (skill.getValue().expiration > 0) {
                    skillsWithExpiration.put(skill.getKey().getId(), skill.getValue().expiration);
                }
                if (skill.getKey().isFourthJob()) {
                    skillsWithMax.put(skill.getKey().getId(), skill.getValue().masterlevel);
                }
            }

            int amount = skillsWithoutMax.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Integer> x : skillsWithoutMax.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeInt(x.getValue()); // 80000000, 80000001, 80001040 show cid if linked.
            }
            mplew.writeShort(0); // For each, int

            amount = skillsWithExpiration.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Long> x : skillsWithExpiration.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeLong(x.getValue()); // Probably expiring skills here
            }
            mplew.writeShort(0); // For each, int

            amount = skillsWithMax.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Integer> x : skillsWithMax.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeInt(x.getValue());
            }
            mplew.writeShort(0); // For each, int (Master level = 0? O.O)
        }
    }

    public static void addCoolDownInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        mplew.writeShort(cd.size());
        for (final MapleCoolDownValueHolder cooling : cd) {
            mplew.writeInt(cooling.skillId);
            mplew.writeShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static void addRocksInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final int[] mapz = chr.getRegRocks();
        for (int i = 0; i < 5; i++) { // VIP teleport map
            mplew.writeInt(mapz[i]);
        }

        final int[] map = chr.getRocks();
        for (int i = 0; i < 10; i++) { // VIP teleport map
            mplew.writeInt(map[i]);
        }

        final int[] maps = chr.getHyperRocks();
        for (int i = 0; i < 13; i++) { // VIP teleport map
            mplew.writeInt(maps[i]);
        }
        for (int i = 0; i < 13; i++) { // VIP teleport map
            mplew.writeInt(maps[i]);
        }
    }

    public static void addRingInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeShort(0); // 0x400
        //01 00 = size
        //01 00 00 00 = gametype?
        //03 00 00 00 = win
        //00 00 00 00 = tie/loss
        //01 00 00 00 = tie/loss
        //16 08 00 00 = points

        // 0x800
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        mplew.writeShort(cRing.size());
        for (MapleRing ring : cRing) { // 33
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        mplew.writeShort(fRing.size());
        for (MapleRing ring : fRing) { // 37
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        mplew.writeShort(mRing.size());
        int marriageId = 30000;
        for (MapleRing ring : mRing) { // 48
            mplew.writeInt(marriageId);
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeShort(3); //1 = engaged 3 = married
            // mplew.writeInt(ring.getItemId());
            // mplew.writeInt(ring.getItemId());
            mplew.writeInt(ring.getRingId());
            mplew.writeInt(ring.getPartnerRingId());
            mplew.writeAsciiString(chr.getGender() == 0 ? chr.getName() : ring.getPartnerName(), 13);
            mplew.writeAsciiString(chr.getGender() == 0 ? ring.getPartnerName() : chr.getName(), 13);
            // mplew.writeAsciiString(ring.getPartnerName(), 13);
            // mplew.writeAsciiString(ring.getPartnerName(), 13);
        }
    }

    public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso()); // mesos
        mplew.writeInt(0); // 4 ints per size
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots

        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)); // 0x200000 : int + int actually
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
            mplew.writeLong(getTime(Long.parseLong(stat.getCustomData())));
        } else {
            mplew.writeLong(getTime(-2));
        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); //start of evan equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); //start of mechanic equips, ty KDMS
        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of android equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        
        mplew.writeShort(0); // start of totem inventory
        for (Item item : equipped) {
            if ((item.getPosition() <= -5000 && item.getPosition() >= -5003)) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of use inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            if (item.getPosition() < 100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.write(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of extended slots
        for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
            mplew.writeInt(i);
            mplew.writeInt(chr.getExtendedSlot(i));
            for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
                if (item.getPosition() > (i * 100 + 100) && item.getPosition() < (i * 100 + 200)) {
                    addItemPosition(mplew, item, false, true);
                    addItemInfo(mplew, item, chr);
                }
            }
            mplew.writeInt(-1);
        }
        mplew.writeInt(-1);
        mplew.writeInt(0); // 0x40000000 Foreach : Int + Long
        mplew.writeInt(0); // 0x400 Foreach : Long + Long
        mplew.write(0); // 0x20000000 if got, then below
		/*mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);		
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        
        mplew.write(0); // a boolean
         */
    }

    public static void addCharStats(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(chr.getId()); 
        mplew.writeAsciiString(chr.getName(), 13);
        mplew.write(chr.getGender()); 
        mplew.write(chr.getSkinColor()); 
        mplew.writeInt(chr.getFace()); 
        mplew.writeInt(chr.getHair()); 
        mplew.writeZeroBytes(24); 
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob()); 
        chr.getStat().connectData(mplew);
        mplew.writeShort(0); 
        if (GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob()) || GameConstants.isJett(chr.getJob()) || GameConstants.isPhantom(chr.getJob()) || GameConstants.isMihile(chr.getJob())) {
            final int size = chr.getRemainingSpSize();
            mplew.write(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.write(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp());
        }
        mplew.writeInt(chr.getExp());
        mplew.writeInt(chr.getFame()); 
        mplew.writeInt(chr.getGachExp()); 
        mplew.writeInt(chr.getMapId()); 
        mplew.write(chr.getInitialSpawnpoint()); 
        mplew.writeInt(0); 
        mplew.writeShort(chr.getSubcategory()); 
        if (GameConstants.isDemon(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        }
        mplew.write(chr.getFatigue());
        mplew.writeInt(GameConstants.getCurrentDate());
        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeInt(chr.getTrait(t).getTotalExp()); 
        }
        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeShort(0);
        }
        mplew.write(0);
        mplew.writeReversedLong(getTime(System.currentTimeMillis()));
        mplew.writeInt(chr.getStat().pvpExp);
        mplew.write(chr.getStat().pvpRank); 
        mplew.writeInt(chr.getBattlePoints()); 
        mplew.write(5); 
        mplew.writeInt(0); 
        mplew.write(0);
        mplew.writeReversedLong(getTime(-2L));
        mplew.writeInt(0);
        mplew.write(0);
        chr.getCharacterCard().connectData(mplew);
        mplew.writeReversedLong(getTime(System.currentTimeMillis()));
    }

    public static void addCharLook(final MaplePacketLittleEndianWriter mplew, final MapleCharacterLook chr, final boolean mega, MapleClient client) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getJob());
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> equip = chr.getEquips();
        final Map<Byte, Integer> totem = new LinkedHashMap<>();
        for (final Entry<Byte, Integer> item : equip.entrySet()) {
            if (item.getKey() < -127) { //not visible
                continue;
            }
            byte pos = (byte) (item.getKey() * -1);
            
            if (pos <= -118 && pos >= -120) {
                pos = (byte) (pos + 118);
                totem.put(pos, item.getValue());
            } else if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getValue());
            } else if (pos > 100 && pos != 111) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getValue());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of visible itens
        for (final Entry<Byte, Integer> entry : totem.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of totem
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // ending markers

        final Integer cWeapon = equip.get((byte) -111);
        mplew.writeInt(cWeapon != null ? cWeapon : 0);
        mplew.writeBool(chr.isElf(client.getPlayer()));
        mplew.writeZeroBytes(12); // pets
        if ((GameConstants.isDemon(chr.getJob()))) {
            mplew.writeInt(chr.getDemonMarking());
        }
    }

    public static void addExpirationTime(final MaplePacketLittleEndianWriter mplew, final long time) {
        mplew.writeLong(getTime(time));
    }

    public static void addItemPosition(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean trade, final boolean bagSlot) {
        if (item == null) {
            mplew.write(0);
            return;
        }
        short pos = item.getPosition();
        if (pos <= -1) {
            pos *= -1;
            if (pos > 100 && pos < 1000) {
                pos -= 100;
            }
        }
        if (bagSlot) {
            mplew.writeInt((pos % 100) - 1);
        } else if (!trade && item.getType() == 1) {
            mplew.writeShort(pos);
        } else {
            mplew.write(pos);
        }
    }

    public static void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item) {
        addItemInfo(mplew, item, null);
    }

/*      */   public static final void addItemInfo(MaplePacketLittleEndianWriter mplew, Item item, MapleCharacter chr) {
/*  705 */     mplew.write(item.getPet() != null ? 3 : item.getType());
/*  706 */     mplew.writeInt(item.getItemId());
/*  707 */     boolean hasUniqueId = (item.getUniqueId() > 0) && (!GameConstants.isMarriageRing(item.getItemId())) && (item.getItemId() / 10000 != 166);
/*      */ 
/*  709 */     mplew.write(hasUniqueId ? 1 : 0);
/*  710 */     if (hasUniqueId) {
/*  711 */       mplew.writeLong(item.getUniqueId());
/*      */     }
/*  713 */     if (item.getPet() != null) {
/*  714 */       addPetItemInfo(mplew, item, item.getPet(), true);
/*      */     } else {
/*  716 */       addExpirationTime(mplew, item.getExpiration());
/*  717 */       mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(Integer.valueOf(item.getItemId())));
/*  718 */       if (item.getType() == 1) {
/*  719 */         Equip equip = (Equip)item;
/*  720 */         mplew.write(equip.getUpgradeSlots());
/*  721 */         mplew.write(equip.getLevel());
/*  722 */         mplew.writeShort(equip.getStr());
/*  723 */         mplew.writeShort(equip.getDex());
/*  724 */         mplew.writeShort(equip.getInt());
/*  725 */         mplew.writeShort(equip.getLuk());
/*  726 */         mplew.writeShort(equip.getHp());
/*  727 */         mplew.writeShort(equip.getMp());
/*  728 */         mplew.writeShort(equip.getWatk());
/*  729 */         mplew.writeShort(equip.getMatk());
/*  730 */         mplew.writeShort(equip.getWdef());
/*  731 */         mplew.writeShort(equip.getMdef());
/*  732 */         mplew.writeShort(equip.getAcc());
/*  733 */         mplew.writeShort(equip.getAvoid());
/*  734 */         mplew.writeShort(equip.getHands());
/*  735 */         mplew.writeShort(equip.getSpeed());
/*  736 */         mplew.writeShort(equip.getJump());
/*  737 */         mplew.writeMapleAsciiString(equip.getOwner());
/*  738 */         mplew.writeShort(equip.getFlag());
/*  739 */         mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
/*  740 */         mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel()));
/*  741 */         mplew.writeInt(equip.getExpPercentage() * 100000);
/*  742 */         mplew.writeInt(equip.getDurability());
/*  743 */         mplew.writeInt(equip.getViciousHammer());
/*  744 */         mplew.writeShort(equip.getPVPDamage());
/*  745 */         mplew.write(equip.getState());
/*  746 */         mplew.write(equip.getEnhance());
/*  747 */         mplew.writeShort(equip.getPotential1());
/*  748 */         if (!hasUniqueId) {
/*  749 */           mplew.writeShort(equip.getPotential2());
/*  750 */           mplew.writeShort(equip.getPotential3());
/*  751 */           mplew.writeShort(equip.getPotential4());
/*  752 */           mplew.writeShort(equip.getPotential5());
/*      */         }
/*  754 */         mplew.writeShort(equip.getSocketState());
/*  755 */         mplew.writeShort(equip.getSocket1() % 10000);
/*  756 */         mplew.writeShort(equip.getSocket2() % 10000);
/*  757 */         mplew.writeShort(equip.getSocket3() % 10000);
/*  758 */         mplew.writeLong(equip.getInventoryId() <= 0L ? -1L : equip.getInventoryId());
/*  759 */         mplew.writeLong(getTime(-2L));
/*  760 */         mplew.writeInt(-1);
/*      */       } else {
/*  762 */         mplew.writeShort(item.getQuantity());
/*  763 */         mplew.writeMapleAsciiString(item.getOwner());
/*  764 */         mplew.writeShort(item.getFlag());
/*  765 */         if ((GameConstants.isThrowingStar(item.getItemId())) || (GameConstants.isBullet(item.getItemId())) || (item.getItemId() / 10000 == 287))
/*  766 */           mplew.writeLong(item.getInventoryId() <= 0L ? -1L : item.getInventoryId());
/*      */       }
/*      */     }
/*      */   }

    public static void serializeMovementList(final MaplePacketLittleEndianWriter lew, final List<LifeMovementFragment> moves) {
        lew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && (chr.getPlayerShop().getShopType() == 3 || chr.getPlayerShop().getShopType() == 4) && chr.getPlayerShop().isAvailable()) {
            addOmok(mplew, chr.getPlayerShop(), chr);
        } else if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static void addInteraction(final MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType());
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != 1) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        }
        mplew.write(shop.getItemId() % 10);
        mplew.write(shop.getSize()); //current size
        mplew.write(shop.getMaxSize()); //full slots... 4 = 4-1=3 = has slots, 1-1=0 = no slots
        if (shop.getShopType() != 1) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }
    }
    
    public static void addOmok(final MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop, MapleCharacter owner) {
        mplew.write(shop.getGameType()); // 1 = omok, 2 = matchcard
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        mplew.write(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        mplew.write(shop.getGameType() == 1 ? 0 : owner.getMatchCardVal()); 
        mplew.write(shop.getSize()); 
        mplew.write(2); 
        mplew.write(shop.isOpen() ? 0 : 1);
    }

    public static void addCharacterInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(-1);
        mplew.writeInt(-3);
        mplew.writeZeroBytes(8); //5 bytes v99 [byte] [byte] [int] [byte]
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity());
        if (chr.getBlessOfFairyOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
        } else {
            mplew.write(0);
        }
        if (chr.getBlessOfEmpressOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
        } else {
            mplew.write(0);
        }
        final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(ultExplorer.getCustomData());
        } else {
            mplew.write(0);
        }
        addInventoryInfo(mplew, chr);
        addSkillInfo(mplew, chr); // 0x100
        addCoolDownInfo(mplew, chr); // 0x8000
        addQuestInfo(mplew, chr);
        addRingInfo(mplew, chr);
        addRocksInfo(mplew, chr); // 0x1000
        addMonsterBookInfo(mplew, chr);
        mplew.writeShort(0);
        mplew.writeShort(0); // New year gift card size // 0x40000
        chr.QuestInfoPacket(mplew); // 0x80000
        if (chr.getJob() >= 3300 && chr.getJob() <= 3312) { // 0x400000
            addJaguarInfo(mplew, chr);
        }
/* 655 */     mplew.writeShort(0);
/* 656 */     mplew.writeShort(0);
/* 657 */     addPhantomSkills(mplew, chr);
               addInnerStats(mplew, chr);
               addCoreAura(mplew, chr);
               mplew.writeShort(0); 
               mplew.writeZeroBytes(48); 
    }
    
/*     */   public static final void addPhantomSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
/* 664 */     Map x = chr.sortPhantomSkills();
/* 665 */     for (int i = 1; i <= 4; i++) {
/* 666 */       List skillids = (List)x.get(Integer.valueOf(i));
/* 667 */       for (int z = 0; z < GameConstants.getPhantomBookSlot(i); z++) {
/* 668 */         mplew.writeInt(((Integer)skillids.get(z)).intValue());
/*     */       }
/*     */     }
/* 671 */     Map equipped = chr.getEquippedSkills();
/* 672 */     for (int i = 1; i <= 4; i++)
/* 673 */       mplew.writeInt(equipped.get(Integer.valueOf(i)) == null ? 0 : ((Integer)equipped.get(Integer.valueOf(i))).intValue());
/*     */   }

/*     */ 
        public static final void addInnerStats(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<InnerSkillValueHolder> skills = chr.getInnerSkills();
        mplew.writeShort(skills.size());
        for (int i = 0; i < skills.size(); ++i) {
            mplew.write(i + 1); // key
            mplew.writeInt(skills.get(i).getSkillId()); //d 7000000 id ++, 71 = char cards
            mplew.write(skills.get(i).getSkillLevel()); // level
            mplew.write(skills.get(i).getRank()); //rank, C, B, A, and S
        }
        
        mplew.writeInt(chr.getHonourLevel()); //honor lvl
        mplew.writeInt(chr.getHonourExp()); //honor exp
    }
                 
  public static final void addCoreAura(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
    mplew.writeInt(0);
    mplew.writeLong(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeInt(0);
    mplew.writeLong(getTime(System.currentTimeMillis() + 86400000));
    mplew.writeInt(0);
    mplew.write((GameConstants.isJett(chr.getJob())) ? 1 : 0);
  }
/*     */ 

    public static void addMonsterBookInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(0); // 0x20000
        if (chr.getMonsterBook().getSetScore() > 0) { // 0x10000
            chr.getMonsterBook().writeFinished(mplew);
        } else {
            chr.getMonsterBook().writeUnfinished(mplew);
        }

        mplew.writeInt(chr.getMonsterBook().getSet()); // 0x80000000
    }

    public static void addPetItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final MaplePet pet, final boolean active) {
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        mplew.writeInt(-1);
        mplew.writeAsciiString(pet.getName(), 13);
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        mplew.writeShort(0);
        mplew.writeShort(pet.getFlags());
        mplew.writeInt(pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0 ? pet.getSecondsLeft() : 0); //in seconds, 3600 = 1 hr.
        mplew.writeShort(0);
        mplew.write(active ? (pet.getSummoned() ? pet.getSummonedValue() : 0) : 0); // 1C 5C 98 C6 01
        for (int i = 0; i < 4; i++) {
            mplew.write(0); //0x40 before, changed to 0?
        }
    }

   
/*      */   public static void addShopInfo(MaplePacketLittleEndianWriter mplew, MapleShop shop, MapleClient c)
/*      */   {
/* 1002 */     MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
/* 1003 */     mplew.write(shop.getRanks().size() > 0 ? 1 : 0);
/*      */ 
/* 1006 */     if (shop.getRanks().size() > 0) {
/* 1007 */       mplew.write(shop.getRanks().size());
/* 1008 */       for (Pair s : shop.getRanks()) {
/* 1009 */         mplew.writeInt(((Integer)s.left).intValue());
/* 1010 */         mplew.writeMapleAsciiString((String)s.right);
/*      */       }
/*      */     }
/* 1013 */     mplew.writeShort(shop.getItems().size() + c.getPlayer().getRebuy().size());
/* 1014 */     for (MapleShopItem item : shop.getItems()) {
/* 1015 */       addShopItemInfo(mplew, item, shop, ii, null);
/*      */     }
        for (Iterator<Item> it = c.getPlayer().getRebuy().iterator(); it.hasNext();) {
            Item i = it.next();
            addShopItemInfo(mplew, new MapleShopItem(i.getItemId(), (int)ii.getPrice(i.getItemId())), shop, ii, i);
        }
   }
/*      */ 
/*      */   public static void addShopItemInfo(MaplePacketLittleEndianWriter mplew, MapleShopItem item, MapleShop shop, MapleItemInformationProvider ii, Item i)
/*      */   {
/* 1036 */     mplew.writeInt(item.getItemId());
/* 1037 */     mplew.writeInt(item.getPrice());
/* 1038 */     mplew.write(0);
/* 1039 */     mplew.writeInt(item.getReqItem());
/* 1040 */     mplew.writeInt(item.getReqItemQ());
/* 1041 */     mplew.writeInt(item.getExpiration());
/* 1042 */     mplew.writeInt(item.getMinLevel());
/* 1043 */     mplew.writeInt(item.getCategory());
/* 1044 */     mplew.write(0);
/* 1045 */     mplew.writeInt(0);
/* 1046 */     mplew.writeInt(0);
/* 1047 */     if ((!GameConstants.isThrowingStar(item.getItemId())) && (!GameConstants.isBullet(item.getItemId()))) {
/* 1048 */       mplew.writeShort(1);
/* 1049 */       mplew.writeShort(1000);
/*      */     } else {
/* 1051 */       mplew.writeZeroBytes(6);
/* 1052 */       mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
/* 1053 */       mplew.writeShort(ii.getSlotMax(item.getItemId()));
/*      */     }
/*      */ 
/* 1073 */     mplew.write(i == null ? 0 : 1);
/* 1074 */     if (i != null) {
/* 1075 */       addItemInfo(mplew, i);
/*      */     }
/* 1077 */     if (shop.getRanks().size() > 0) {
/* 1078 */       mplew.write(item.getRank() >= 0 ? 1 : 0);
/* 1079 */       if (item.getRank() >= 0) {
/* 1080 */         mplew.write(item.getRank());
/*      */       }
/*      */     }
/* 1083 */     mplew.writeZeroBytes(16);
/* 1085 */     for (int j = 0; j < 4; j++) {
        mplew.writeReversedLong(System.currentTimeMillis());
    }
/*      */   }

    public static void addJaguarInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
      //  mplew.write(chr.getIntNoRecord(GameConstants.JAGUAR));
        mplew.write(0x28); // color?
        mplew.writeZeroBytes(20); //probably mobID of the 5 mobs that can be captured.
    }

    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = GameConstants.MAX_BUFFSTAT; i >= 1; i--) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition() - 1] |= statup.left.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }
}
