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
package handling.channel.handler;

import client.MapleTrait.MapleTraitType;
import client.*;
import client.SkillFactory.CraftingEntry;
import client.inventory.MapleImp.ImpFlag;
import client.inventory.*;
import constants.GameConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.ItemMakerFactory.GemCreateEntry;
import server.ItemMakerFactory.ItemMakerCreateEntry;
import server.*;
import server.maps.MapleExtractor;
import server.maps.MapleReactor;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;

public class ItemMakerHandler {

    private static final Map<String, Integer> craftingEffects = new HashMap<>();

    static {
    /*    craftingEffects.put("Effect/BasicEff.img/professions/herbalism", 92000000);
        craftingEffects.put("Effect/BasicEff.img/professions/mining", 92010000);
        craftingEffects.put("Effect/BasicEff.img/professions/herbalismExtract", 92000000);
        craftingEffects.put("Effect/BasicEff.img/professions/miningExtract", 92010000);

        craftingEffects.put("Effect/BasicEff.img/professions/equip_product", 92020000);
        craftingEffects.put("Effect/BasicEff.img/professions/acc_product", 92030000);
        craftingEffects.put("Effect/BasicEff.img/professions/alchemy", 92040000);
        * 
        */
    }

    public static enum CraftRanking {

        SOSO(19, 30),
        GOOD(20, 40),
        COOL(21, 50);
        public int i, craft;

        private CraftRanking(int i, int craft) {
            this.i = i;
            this.craft = craft;
        }
    }

    public static void ItemMaker(final LittleEndianAccessor slea, final MapleClient c) {
        //System.out.println(slea.toString()); //change?
        final int makerType = slea.readInt();

        switch (makerType) {
            case 1: { // Gem
                final int toCreate = slea.readInt();

                if (GameConstants.isGem(toCreate)) {
                    final GemCreateEntry gem = ItemMakerFactory.getInstance().getGemInfo(toCreate);
                    if (gem == null) {
                        return;
                    }
                    if (!hasSkill(c, gem.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < gem.getCost()) {
                        return; // H4x
                    }
                    final int randGemGiven = getRandomGem(gem.getRandomReward());

                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(randGemGiven)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    final int taken = checkRequiredNRemove(c, gem.getReqRecipes());
                    if (taken == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-gem.getCost(), false);
                    MapleInventoryManipulator.addById(c, randGemGiven, (byte) (taken == randGemGiven ? 9 : 1), "Made by Gem " + toCreate + " on " + FileoutputUtil.CurrentReadable_Date()); // Gem is always 1

                    c.getSession().write(EffectPacket.ItemMaker_Success());
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
                } else if (GameConstants.isOtherGem(toCreate)) {
                    //non-gems that are gems
                    //stim and numEnchanter always 0
                    final GemCreateEntry gem = ItemMakerFactory.getInstance().getGemInfo(toCreate);
                    if (gem == null) {
                        return;
                    }
                    if (!hasSkill(c, gem.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < gem.getCost()) {
                        return; // H4x
                    }

                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(toCreate)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    if (checkRequiredNRemove(c, gem.getReqRecipes()) == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-gem.getCost(), false);
                    if (GameConstants.getInventoryType(toCreate) == MapleInventoryType.EQUIP) {
                        MapleInventoryManipulator.addbyItem(c, MapleItemInformationProvider.getInstance().getEquipById(toCreate));
                    } else {
                        MapleInventoryManipulator.addById(c, toCreate, (byte) 1, "Made by Gem " + toCreate + " on " + FileoutputUtil.CurrentReadable_Date()); // Gem is always 1
                    }

                    c.getSession().write(EffectPacket.ItemMaker_Success());
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
                } else {
                    final boolean stimulator = slea.readByte() > 0;
                    final int numEnchanter = slea.readInt();

                    final ItemMakerCreateEntry create = ItemMakerFactory.getInstance().getCreateInfo(toCreate);
                    if (create == null) {
                        return;
                    }
                    if (numEnchanter > create.getTUC()) {
                        return; // h4x
                    }
                    if (!hasSkill(c, create.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < create.getCost()) {
                        return; // H4x
                    }
                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(toCreate)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    if (checkRequiredNRemove(c, create.getReqItems()) == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-create.getCost(), false);

                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    final Equip toGive = (Equip) ii.getEquipById(toCreate);

                    if (stimulator || numEnchanter > 0) {
                        if (c.getPlayer().haveItem(create.getStimulator(), 1, false, true)) {
                            ii.randomizeStats_Above(toGive);
                            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, create.getStimulator(), 1, false, false);
                        }
                        for (int i = 0; i < numEnchanter; i++) {
                            final int enchant = slea.readInt();
                            if (c.getPlayer().haveItem(enchant, 1, false, true)) {
                                final Map<String, Integer> stats = ii.getEquipStats(enchant);
                                if (stats != null) {
                                    addEnchantStats(stats, toGive);
                                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, enchant, 1, false, false);
                                }
                            }
                        }
                    }
                    if (!stimulator || Randomizer.nextInt(10) != 0) {
                        MapleInventoryManipulator.addbyItem(c, toGive);
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
                    } else {
                        c.getPlayer().dropMessage(5, "The item was overwhelmed by the stimulator.");
                    }
                    c.getSession().write(EffectPacket.ItemMaker_Success());

                }
                break;
            }
            case 3: { // Making Crystals
                final int etc = slea.readInt();
                if (c.getPlayer().haveItem(etc, 100, false, true)) {
                    MapleInventoryManipulator.addById(c, getCreateCrystal(etc), (short) 1, "Made by Maker " + etc + " on " + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, etc, 100, false, false);

                    c.getSession().write(EffectPacket.ItemMaker_Success());
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
                }
                break;
            }
            case 4: { // Disassembling EQ.
                final int itemId = slea.readInt();
                slea.readInt();
                final byte slot = (byte) slea.readInt();

                final Item toUse = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
                if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
                    return;
                }
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                if (!ii.isAccountShared(itemId)) {
                    final int[] toGive = getCrystal(itemId, ii.getReqLevel(itemId));
                    MapleInventoryManipulator.addById(c, toGive[0], (byte) toGive[1], "Made by disassemble " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, slot, (byte) 1, false);
                }
                c.getSession().write(EffectPacket.ItemMaker_Success());
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
                break;
            }
        }
    }

    private static int getCreateCrystal(final int etc) {
        int itemid;
        final short level = MapleItemInformationProvider.getInstance().getItemMakeLevel(etc);

        if (level >= 31 && level <= 50) {
            itemid = 4260000;
        } else if (level >= 51 && level <= 60) {
            itemid = 4260001;
        } else if (level >= 61 && level <= 70) {
            itemid = 4260002;
        } else if (level >= 71 && level <= 80) {
            itemid = 4260003;
        } else if (level >= 81 && level <= 90) {
            itemid = 4260004;
        } else if (level >= 91 && level <= 100) {
            itemid = 4260005;
        } else if (level >= 101 && level <= 110) {
            itemid = 4260006;
        } else if (level >= 111 && level <= 120) {
            itemid = 4260007;
        } else if (level >= 121) {
            itemid = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker id");
        }
        return itemid;
    }

    private static int[] getCrystal(final int itemid, final int level) {
        int[] all = new int[2];
        all[0] = -1;
        if (level >= 31 && level <= 50) {
            all[0] = 4260000;
        } else if (level >= 51 && level <= 60) {
            all[0] = 4260001;
        } else if (level >= 61 && level <= 70) {
            all[0] = 4260002;
        } else if (level >= 71 && level <= 80) {
            all[0] = 4260003;
        } else if (level >= 81 && level <= 90) {
            all[0] = 4260004;
        } else if (level >= 91 && level <= 100) {
            all[0] = 4260005;
        } else if (level >= 101 && level <= 110) {
            all[0] = 4260006;
        } else if (level >= 111 && level <= 120) {
            all[0] = 4260007;
        } else if (level >= 121 && level <= 200) {
            all[0] = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker type" + level);
        }
        if (GameConstants.isWeapon(itemid) || GameConstants.isOverall(itemid)) {
            all[1] = Randomizer.rand(5, 11);
        } else {
            all[1] = Randomizer.rand(3, 7);
        }
        return all;
    }

    private static void addEnchantStats(final Map<String, Integer> stats, final Equip item) {
        Integer s = stats.get("PAD");
        if (s != null && s != 0) {
            item.setWatk((short) (item.getWatk() + s));
        }
        s = stats.get("MAD");
        if (s != null && s != 0) {
            item.setMatk((short) (item.getMatk() + s));
        }
        s = stats.get("ACC");
        if (s != null && s != 0) {
            item.setAcc((short) (item.getAcc() + s));
        }
        s = stats.get("EVA");
        if (s != null && s != 0) {
            item.setAvoid((short) (item.getAvoid() + s));
        }
        s = stats.get("Speed");
        if (s != null && s != 0) {
            item.setSpeed((short) (item.getSpeed() + s));
        }
        s = stats.get("Jump");
        if (s != null && s != 0) {
            item.setJump((short) (item.getJump() + s));
        }
        s = stats.get("MaxHP");
        if (s != null && s != 0) {
            item.setHp((short) (item.getHp() + s));
        }
        s = stats.get("MaxMP");
        if (s != null && s != 0) {
            item.setMp((short) (item.getMp() + s));
        }
        s = stats.get("STR");
        if (s != null && s != 0) {
            item.setStr((short) (item.getStr() + s));
        }
        s = stats.get("DEX");
        if (s != null && s != 0) {
            item.setDex((short) (item.getDex() + s));
        }
        s = stats.get("INT");
        if (s != null && s != 0) {
            item.setInt((short) (item.getInt() + s));
        }
        s = stats.get("LUK");
        if (s != null && s != 0) {
            item.setLuk((short) (item.getLuk() + s));
        }
        s = stats.get("randOption");
        if (s != null && s != 0) {
            final int ma = item.getMatk(), wa = item.getWatk();
            if (wa > 0) {
                item.setWatk((short) (Randomizer.nextBoolean() ? (wa + s) : (wa - s)));
            }
            if (ma > 0) {
                item.setMatk((short) (Randomizer.nextBoolean() ? (ma + s) : (ma - s)));
            }
        }
        s = stats.get("randStat");
        if (s != null && s != 0) {
            final int str = item.getStr(), dex = item.getDex(), luk = item.getLuk(), int_ = item.getInt();
            if (str > 0) {
                item.setStr((short) (Randomizer.nextBoolean() ? (str + s) : (str - s)));
            }
            if (dex > 0) {
                item.setDex((short) (Randomizer.nextBoolean() ? (dex + s) : (dex - s)));
            }
            if (int_ > 0) {
                item.setInt((short) (Randomizer.nextBoolean() ? (int_ + s) : (int_ - s)));
            }
            if (luk > 0) {
                item.setLuk((short) (Randomizer.nextBoolean() ? (luk + s) : (luk - s)));
            }
        }
    }

    private static int getRandomGem(final List<Pair<Integer, Integer>> rewards) {
        int itemid;
        final List<Integer> items = new ArrayList<>();

        for (final Pair p : rewards) {
            itemid = (Integer) p.getLeft();
            for (int i = 0; i < (Integer) p.getRight(); i++) {
                items.add(itemid);
            }
        }
        return items.get(Randomizer.nextInt(items.size()));
    }

    private static int checkRequiredNRemove(final MapleClient c, final List<Pair<Integer, Integer>> recipe) {
        int itemid = 0;
        for (final Pair<Integer, Integer> p : recipe) {
            if (!c.getPlayer().haveItem(p.getLeft(), p.getRight(), false, true)) {
                return 0;
            }
        }
        for (final Pair<Integer, Integer> p : recipe) {
            itemid = p.getLeft();
            MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, p.getRight(), false, false);
        }
        return itemid;
    }

    private static boolean hasSkill(final MapleClient c, final int reqlvl) {
        return c.getPlayer().getSkillLevel(SkillFactory.getSkill(PlayerStats.getSkillByJob(1007, c.getPlayer().getJob()))) >= reqlvl;
    }

    public static void UseRecipe(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        slea.readInt();
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 251) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
    }

    public static void MakeExtractor(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int itemId = slea.readInt();
        final int fee = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null || toUse.getQuantity() < 1 || itemId / 10000 != 304 || fee <= 0 || chr.getExtractor() != null || !chr.getMap().isTown()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        chr.setExtractor(new MapleExtractor(chr, itemId, fee, chr.getFH())); //no clue about time left
        chr.getMap().spawnExtractor(chr.getExtractor());

        //expiry date ..
        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, toUse.getPosition(), (short) 1, false);
    }

    public static void UseBag(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
       slea.readInt();
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 433) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        boolean firstTime = !chr.getExtendedSlots().contains(itemId);
        if (firstTime) {
            chr.getExtendedSlots().add(itemId);
            chr.changedExtended();
            short flag = toUse.getFlag();
            flag |= ItemFlag.LOCK.getValue();
            flag |= ItemFlag.UNTRADEABLE.getValue();
            toUse.setFlag(flag);
            c.getSession().write(InventoryPacket.updateSpecialItemUse(toUse, (byte) 4, toUse.getPosition(), true, chr));
        }
        c.getSession().write(CField.openBag(chr.getExtendedSlots().indexOf(itemId), itemId, firstTime));
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void StartHarvest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        //its ok if a hacker bypasses this as we do everything in the reactor anyway
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(slea.readInt());
        if (reactor == null || !reactor.isAlive() || reactor.getReactorId() > 200011 || chr.getStat().harvestingTool <= 0 || reactor.getTruePosition().distanceSq(chr.getTruePosition()) > 10000 || c.getPlayer().getFatigue() >= (GameConstants.GMS ? 200 : 100)) {
            return;
        }
        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) c.getPlayer().getStat().harvestingTool);
        if (item == null || ((Equip) item).getDurability() == 0) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            return;
        }
        MapleQuestStatus marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.HARVEST_TIME));
        if (marr.getCustomData() == null) {
            marr.setCustomData("0");
        }
        long lastTime = Long.parseLong(marr.getCustomData());
        if (lastTime + (5000) > System.currentTimeMillis()) {
            c.getPlayer().dropMessage(5, "You may not harvest yet.");
        } else {
            marr.setCustomData(String.valueOf(System.currentTimeMillis()));
            c.getSession().write(CField.harvestMessage(reactor.getObjectId(), GameConstants.GMS ? 13 : 11)); //ok to harvest, gogo
            c.getPlayer().getMap().broadcastMessage(chr, CField.showHarvesting(chr.getId(), item.getItemId()), false);
        }
    }

    public static void StopHarvest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        //its ok if a hacker bypasses this as we do everything in the reactor anyway
        /*final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(slea.readInt());
        if (reactor == null || !reactor.isAlive() || reactor.getReactorId() > 200011 || chr.getStat().harvestingTool <= 0 || reactor.getTruePosition().distanceSq(chr.getTruePosition()) > 40000.0 || reactor.getState() < 3 || c.getPlayer().getFatigue() >= 100) { //bug in global, so we use this to bug fix
        return;
        }
        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) c.getPlayer().getStat().harvestingTool);
        if (item == null || ((Equip) item).getDurability() == 0) {
        c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        return;
        }
        c.getPlayer().getMap().destroyReactor(reactor.getObjectId());
        ReactorScriptManager.getInstance().act(c, reactor);*/
    }

    public static void ProfessionInfo(final LittleEndianAccessor slea, final MapleClient c) { //so pointless
        try {
            String asdf = slea.readMapleAsciiString();
            int level1 = slea.readInt();
            c.getSession().write(CWvsContext.professionInfo(asdf, level1, slea.readInt(), Math.max(0, 100 - ((level1 + 1) - c.getPlayer().getProfessionLevel(Integer.parseInt(asdf))) * 20)));
        } catch (NumberFormatException nfe) {
        } //idc
    }

    public static void CraftEffect(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr.getMapId() != 910001000 && chr.getMap().getExtractorSize() <= 0) {
            return; //ardent mill
        }
        final String effect = slea.readMapleAsciiString();
        final Integer profession = craftingEffects.get(effect);
        if (profession != null && (c.getPlayer().getProfessionLevel(profession.intValue()) > 0 || (profession == 92040000 && chr.getMap().getExtractorSize() > 0))) {
            int time = slea.readInt();
            if (time > 6000 || time < 3000) {
                time = 4000;
            }
            c.getSession().write(EffectPacket.showOwnCraftingEffect(effect, time, effect.endsWith("Extract") ? 1 : 0));
            chr.getMap().broadcastMessage(chr, EffectPacket.showCraftingEffect(chr.getId(), effect, time, effect.endsWith("Extract") ? 1 : 0), false);
        }
    }

    public static void CraftMake(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr.getMapId() != 910001000 && chr.getMap().getExtractorSize() <= 0) {
            return; //ardent mill
        }
        final int something = slea.readInt(); //no clue what it is, but its between 288 and 305..
        //if (something >= 280 && something <= 310) {
        int time = slea.readInt();
        if (time > 6000 || time < 3000) {
            time = 4000;
        }
        chr.getMap().broadcastMessage(CField.craftMake(chr.getId(), something, time));
        //}
    }

/*     */   public static final void CraftComplete(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr)
/*     */   {
/* 556 */     int craftID = slea.readInt();
/* 557 */     SkillFactory.CraftingEntry ce = SkillFactory.getCraft(craftID);
/* 558 */     MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
/* 559 */     if (((chr.getMapId() == 910001000) || ((craftID == 92049000) && (chr.getMap().getExtractorSize() > 0))) && (ce != null)) { if (chr.getFatigue() < (GameConstants.GMS ? 200 : 100)); } else return;
/*     */ 
/* 562 */     int theLevl = c.getPlayer().getProfessionLevel(craftID / 10000 * 10000);
/* 563 */     if ((theLevl <= 0) && (craftID != 92049000)) {
/* 564 */       return;
/*     */     }
/* 566 */     int toGet = 0; int expGain = 0; int fatigue = 0;
/* 567 */     short quantity = 1;
/* 568 */     CraftRanking cr = CraftRanking.GOOD;
/* 569 */     if (craftID == 92049000) {
/* 570 */       int extractorId = slea.readInt();
/* 571 */       int itemId = slea.readInt();
/* 572 */       long invId = slea.readLong();
/* 573 */       int reqLevel = ii.getReqLevel(itemId);
/* 574 */       Item item = chr.getInventory(MapleInventoryType.EQUIP).findByInventoryId(invId, itemId);
/* 575 */       if ((item == null) || (chr.getInventory(MapleInventoryType.ETC).isFull())) {
/* 576 */         return;
/*     */       }
/* 578 */       if (extractorId <= 0) if (theLevl != 0) { if (theLevl >= (reqLevel > 130 ? 6 : (reqLevel - 30) / 20)); } else return;
/* 580 */       if (extractorId > 0) {
/* 581 */         MapleCharacter extract = chr.getMap().getCharacterById(extractorId);
/* 582 */         if ((extract == null) || (extract.getExtractor() == null)) {
/* 583 */           return;
/*     */         }
/* 585 */         MapleExtractor extractor = extract.getExtractor();
/* 586 */         if (extractor.owner != chr.getId()) {
/* 587 */           if (chr.getMeso() < extractor.fee) {
/* 588 */             return;
/*     */           }
/* 590 */           MapleStatEffect eff = ii.getItemEffect(extractor.itemId);
/* 591 */           if ((eff != null) && (eff.getUseLevel() < reqLevel)) {
/* 592 */             return;
/*     */           }
/* 594 */           chr.gainMeso(-extractor.fee, true);
/* 595 */           MapleCharacter owner = chr.getMap().getCharacterById(extractor.owner);
/* 596 */           if ((owner != null) && (owner.getMeso() < 2147483647 - extractor.fee)) {
/* 597 */             owner.gainMeso(extractor.fee, false);
/*     */           }
/*     */         }
/*     */       }
/* 601 */       toGet = 4031016;
/* 602 */       quantity = (short)Randomizer.rand(3, (GameConstants.isWeapon(itemId)) || (GameConstants.isOverall(itemId)) ? 11 : 7);
/* 603 */       if (reqLevel <= 60)
/* 604 */         toGet = 4021013;
/* 605 */       else if (reqLevel <= 90)
/* 606 */         toGet = 4021014;
/* 607 */       else if (reqLevel <= 120) {
/* 608 */         toGet = 4021015;
/*     */       }
/* 610 */       if (quantity <= 5) {
/* 611 */         cr = CraftRanking.SOSO;
/*     */       }
/* 613 */       if ((Randomizer.nextInt(5) == 0) && (toGet != 4031016)) {
/* 614 */         toGet++;
/* 615 */         quantity = 1;
/* 616 */         cr = CraftRanking.COOL;
/*     */       }
/* 618 */       fatigue = 3;
/* 619 */       MapleInventoryManipulator.addById(c, toGet, quantity, "Made by disassemble " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
/* 620 */       MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, item.getPosition(), (byte) 1, false);
/* 621 */     } else if (craftID == 92049001) {
/* 622 */       int itemId = slea.readInt();
/* 623 */       long invId1 = slea.readLong();
/* 624 */       long invId2 = slea.readLong();
/* 625 */       int reqLevel = ii.getReqLevel(itemId);
/* 626 */       Equip item1 = (Equip)chr.getInventory(MapleInventoryType.EQUIP).findByInventoryIdOnly(invId1, itemId);
/* 627 */       Equip item2 = (Equip)chr.getInventory(MapleInventoryType.EQUIP).findByInventoryIdOnly(invId2, itemId);
/* 628 */       for (short i = 0; i < chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit(); i = (short)(i + 1)) {
/* 629 */         Item item = chr.getInventory(MapleInventoryType.EQUIP).getItem(i);
/* 630 */         if ((item != null) && (item.getItemId() == itemId) && (item != item1) && (item != item2)) {
/* 631 */           if (item1 == null) {
/* 632 */             item1 = (Equip)item;
/* 633 */           } else if (item2 == null) {
/* 634 */             item2 = (Equip)item;
/* 635 */             break;
/*     */           }
/*     */         }
/*     */       }
/* 639 */       if ((item1 == null) || (item2 == null)) {
/* 640 */         return;
/*     */       }
/* 642 */       if (theLevl < (reqLevel > 130 ? 6 : (reqLevel - 30) / 20)) {
/* 643 */         return;
/*     */       }
/* 645 */       int potentialState = 17; int potentialChance = theLevl * 2;
/* 646 */       if ((item1.getState() > 0) && (item2.getState() > 0))
/* 647 */         potentialChance = 100;
/* 648 */       else if ((item1.getState() > 0) || (item2.getState() > 0)) {
/* 649 */         potentialChance *= 2;
/*     */       }
/* 651 */       if ((item1.getState() == item2.getState()) && (item1.getState() > 17)) {
/* 652 */         potentialState = item1.getState();
/*     */       }
/*     */ 
/* 655 */       Equip newEquip = ii.fuse(item1.getLevel() > 0 ? (Equip)ii.getEquipById(itemId) : item1, item2.getLevel() > 0 ? (Equip)ii.getEquipById(itemId) : item2);
/* 656 */       int newStat = ii.getTotalStat(newEquip);
/* 657 */       if ((newStat > ii.getTotalStat(item1)) || (newStat > ii.getTotalStat(item2)))
/* 658 */         cr = CraftRanking.COOL;
/* 659 */       else if ((newStat < ii.getTotalStat(item1)) || (newStat < ii.getTotalStat(item2))) {
/* 660 */         cr = CraftRanking.SOSO;
/*     */       }
/* 662 */       if (Randomizer.nextInt(100) < ((newEquip.getUpgradeSlots() > 0) || (potentialChance >= 100) ? potentialChance : potentialChance / 2)) {
/* 663 */         newEquip.resetPotential_Fuse(theLevl > 5, potentialState);
/*     */       }
/* 665 */       newEquip.setFlag((short)ItemFlag.CRAFTED.getValue());
/* 666 */       newEquip.setOwner(chr.getName());
/* 667 */       toGet = newEquip.getItemId();
/* 668 */       expGain = (60 - (theLevl - 1) * 2) * (GameConstants.GMS ? 2 : 1);
/* 669 */       fatigue = 3;
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, item1.getPosition(), (byte) 1, false);
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, item2.getPosition(), (byte) 1, false);
/* 672 */       MapleInventoryManipulator.addbyItem(c, newEquip);
/*     */     } else {
/* 674 */       if ((ce.needOpenItem) && (chr.getSkillLevel(craftID) <= 0)) {
/* 675 */         return;
/*     */       }
/* 677 */       for (Map.Entry e : ce.reqItems.entrySet()) {
/* 678 */         if (!chr.haveItem(((Integer)e.getKey()).intValue(), ((Integer)e.getValue()).intValue())) {
/* 679 */           return;
/*     */         }
/*     */       }
/* 682 */       for (Triple i : ce.targetItems) {
/* 683 */         if (!MapleInventoryManipulator.checkSpace(c, ((Integer)i.left).intValue(), ((Integer)i.mid).intValue(), "")) {
/* 684 */           return;
/*     */         }
/*     */       }
/* 687 */       for (Map.Entry e : ce.reqItems.entrySet()) {
/* 688 */         MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(((Integer)e.getKey()).intValue()), ((Integer)e.getKey()).intValue(), ((Integer)e.getValue()).intValue(), false, false);
/*     */       }
/* 690 */       if ((Randomizer.nextInt(100) < 100 - (ce.reqSkillLevel - theLevl) * 20) || (craftID / 10000 <= 9201)) {
/* 691 */         Map sa = new HashMap();
/*     */         while (true) {
/* 693 */           boolean passed = false;
/* 694 */           for (Triple i : ce.targetItems) {
/* 695 */             if (Randomizer.nextInt(100) < ((Integer)i.right).intValue()) {
/* 696 */               toGet = ((Integer)i.left).intValue();
/* 697 */               quantity = ((Integer)i.mid).shortValue();
/* 698 */               Item receive = null;
/* 699 */               if (GameConstants.getInventoryType(toGet) == MapleInventoryType.EQUIP) {
/* 700 */                 Equip first = (Equip)ii.getEquipById(toGet);
/* 701 */                 if (Randomizer.nextInt(100) < theLevl * 2) {
/* 702 */                   first = ii.randomizeStats(first);
/* 703 */                   cr = CraftRanking.COOL;
/*     */                 }
/* 705 */                 if (Randomizer.nextInt(100) < theLevl * (first.getUpgradeSlots() > 0 ? 2 : 1)) {
/* 706 */                   first.resetPotential();
/* 707 */                   cr = CraftRanking.COOL;
/*     */                 }
/* 709 */                 receive = first;
/* 710 */                 receive.setFlag((short)ItemFlag.CRAFTED.getValue());
/*     */               } else {
/* 712 */                 receive = new Item(toGet, (short) 0, quantity, (short) (ItemFlag.CRAFTED_USE.getValue()));
/*     */               }
/* 714 */               if (ce.period > 0) {
/* 715 */                 receive.setExpiration(System.currentTimeMillis() + ce.period * 60000);
/*     */               }
/* 717 */               receive.setOwner(chr.getName());
/* 718 */               receive.setGMLog("Crafted from " + craftID + " on " + FileoutputUtil.CurrentReadable_Date());
/* 719 */               MapleInventoryManipulator.addFromDrop(c, receive, true, false);
/* 720 */               if (ce.needOpenItem) {
/* 721 */                 int mLevel = chr.getMasterLevel(craftID);
/* 722 */                 if (mLevel == 1)
/* 723 */                   sa.put(ce, new SkillEntry(0, 0, SkillFactory.getDefaultSExpiry(ce)));
/* 724 */                 else if (mLevel > 1) {
/* 725 */                   sa.put(ce, new SkillEntry(2147483647,(chr.getMasterLevel(craftID) - 1), SkillFactory.getDefaultSExpiry(ce)));
/*     */                 }
/*     */               }
/* 728 */               fatigue = ce.incFatigability;
/* 729 */               expGain = ce.incSkillProficiency == 0 ? (fatigue * 20 - (ce.reqSkillLevel - theLevl) * 2) * (GameConstants.GMS ? 2 : 1) : ce.incSkillProficiency;
/* 730 */               chr.getTrait(MapleTrait.MapleTraitType.craft).addExp(cr.craft, chr);
/* 731 */               passed = true;
/* 732 */               break;
/*     */             }
/*     */           }
/* 735 */           if (passed) {
/*     */             break;
/*     */           }
/*     */         }
/* 739 */         chr.changeSkillsLevel(sa);
/*     */       } else {
/* 741 */         quantity = 0;
/* 742 */         cr = CraftRanking.SOSO;
/*     */       }
/*     */     }
/* 745 */     if ((expGain > 0) && (theLevl < 10)) {
/* 746 */       expGain *= chr.getClient().getWorldServer().getTraitRate();
/* 747 */       if (Randomizer.nextInt(100) < chr.getTrait(MapleTrait.MapleTraitType.craft).getLevel() / 5) {
/* 748 */         expGain *= 2;
/*     */       }
/* 750 */       String s = "Alchemy";
/* 751 */       switch (craftID / 10000) {
/*     */       case 9200:
/* 753 */         s = "Herbalism";
/* 754 */         break;
/*     */       case 9201:
/* 756 */         s = "Mining";
/* 757 */         break;
/*     */       case 9202:
/* 759 */         s = "Smithing";
/* 760 */         break;
/*     */       case 9203:
/* 762 */         s = "Accessory Crafting";
/*     */       }
/*     */ 
/* 765 */       chr.dropMessage(-5, s + "'s mastery increased. (+" + expGain + ")");
/* 766 */       if (chr.addProfessionExp(craftID / 10000 * 10000, expGain))
/* 767 */         chr.dropMessage(-5, s + " has gained a level.");
/*     */     }
/*     */     else {
/* 770 */       expGain = 0;
/*     */     }
/* 772 */     MapleQuest.getInstance(2550).forceStart(c.getPlayer(), 9031000, "1");
/* 773 */     chr.setFatigue((byte)(chr.getFatigue() + fatigue));
/* 774 */     chr.getMap().broadcastMessage(CField.craftFinished(chr.getId(), craftID, cr.i, toGet, quantity, expGain));
/*     */   }

    public static void UsePot(final LittleEndianAccessor slea, final MapleClient c) {
        final int itemid = slea.readInt();
        final Item slot = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        if (slot == null || slot.getQuantity() <= 0 || slot.getItemId() != itemid || itemid / 10000 != 244 || MapleItemInformationProvider.getInstance().getPot(itemid) == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getSession().write(CWvsContext.enableActions());
        for (int i = 0; i < c.getPlayer().getImps().length; i++) {
            if (c.getPlayer().getImps()[i] == null) {
                c.getPlayer().getImps()[i] = new MapleImp(itemid);
                c.getSession().write(CWvsContext.updateImp(c.getPlayer().getImps()[i], ImpFlag.SUMMONED.getValue(), i, false));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot.getPosition(), (short) 1, false, false);
                return;
            }
        }

    }

    public static void ClearPot(final LittleEndianAccessor slea, final MapleClient c) {
        final int index = slea.readInt() - 1;
        if (index < 0 || index >= c.getPlayer().getImps().length || c.getPlayer().getImps()[index] == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getSession().write(CWvsContext.updateImp(c.getPlayer().getImps()[index], ImpFlag.REMOVED.getValue(), index, false));
        c.getPlayer().getImps()[index] = null;
    }

    public static void FeedPot(final LittleEndianAccessor slea, final MapleClient c) {
        final int itemid = slea.readInt();
        final Item slot = c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getItem((short) slea.readInt());
        if (slot == null || slot.getQuantity() <= 0 || slot.getItemId() != itemid) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int level = GameConstants.getInventoryType(itemid) == MapleInventoryType.ETC ? MapleItemInformationProvider.getInstance().getItemMakeLevel(itemid) : MapleItemInformationProvider.getInstance().getReqLevel(itemid);
        if (level <= 0 || level < (Math.min(120, c.getPlayer().getLevel()) - 50) || (GameConstants.getInventoryType(itemid) != MapleInventoryType.ETC && GameConstants.getInventoryType(itemid) != MapleInventoryType.EQUIP)) {
            c.getPlayer().dropMessage(1, "The item must be within 50 levels of you.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int index = slea.readInt() - 1;
        if (index < 0 || index >= c.getPlayer().getImps().length || c.getPlayer().getImps()[index] == null || c.getPlayer().getImps()[index].getLevel() >= (MapleItemInformationProvider.getInstance().getPot(c.getPlayer().getImps()[index].getItemId()).right - 1) || c.getPlayer().getImps()[index].getState() != 1) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int mask = ImpFlag.FULLNESS.getValue();
        mask |= ImpFlag.FULLNESS_2.getValue();
        mask |= ImpFlag.UPDATE_TIME.getValue();
        mask |= ImpFlag.AWAKE_TIME.getValue();
        //this is where the magic happens
        c.getPlayer().getImps()[index].setFullness(c.getPlayer().getImps()[index].getFullness() + (100 * (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP ? 2 : 1)));
        if (Randomizer.nextBoolean()) {
            mask |= ImpFlag.CLOSENESS.getValue();
            c.getPlayer().getImps()[index].setCloseness(c.getPlayer().getImps()[index].getCloseness() + 1 + (Randomizer.nextInt(5 * (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP ? 2 : 1))));
        } else if (Randomizer.nextInt(5) == 0) { //1/10 chance of sickness
            c.getPlayer().getImps()[index].setState(4); //sick
            mask |= ImpFlag.STATE.getValue();
        }
        if (c.getPlayer().getImps()[index].getFullness() >= 1000) {
            c.getPlayer().getImps()[index].setState(1);
            c.getPlayer().getImps()[index].setFullness(0);
            c.getPlayer().getImps()[index].setLevel(c.getPlayer().getImps()[index].getLevel() + 1);
            mask |= ImpFlag.SUMMONED.getValue();
            if (c.getPlayer().getImps()[index].getLevel() >= (MapleItemInformationProvider.getInstance().getPot(c.getPlayer().getImps()[index].getItemId()).right - 1)) {
                c.getPlayer().getImps()[index].setState(5);
            }
        }
        MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemid), slot.getPosition(), (short) 1, false, false);
        c.getSession().write(CWvsContext.updateImp(c.getPlayer().getImps()[index], mask, index, false));
    }

    public static void CurePot(final LittleEndianAccessor slea, final MapleClient c) {
        final int itemid = slea.readInt();
        final Item slot = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) slea.readInt());
        if (slot == null || slot.getQuantity() <= 0 || slot.getItemId() != itemid || itemid / 10000 != 434) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int index = slea.readInt() - 1;
        if (index < 0 || index >= c.getPlayer().getImps().length || c.getPlayer().getImps()[index] == null || c.getPlayer().getImps()[index].getState() != 4) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().getImps()[index].setState(1);
        c.getSession().write(CWvsContext.updateImp(c.getPlayer().getImps()[index], ImpFlag.STATE.getValue(), index, false));
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot.getPosition(), (short) 1, false, false);
    }

    public static void RewardPot(final LittleEndianAccessor slea, final MapleClient c) {
        final int index = slea.readInt() - 1;
        if (index < 0 || index >= c.getPlayer().getImps().length || c.getPlayer().getImps()[index] == null || c.getPlayer().getImps()[index].getLevel() < (MapleItemInformationProvider.getInstance().getPot(c.getPlayer().getImps()[index].getItemId()).right - 1)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int itemid = GameConstants.getRewardPot(c.getPlayer().getImps()[index].getItemId(), c.getPlayer().getImps()[index].getCloseness());
        if (itemid <= 0 || !MapleInventoryManipulator.checkSpace(c, itemid, (short) 1, "")) {
            c.getPlayer().dropMessage(1, "Please make some space.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Item Pot from " + c.getPlayer().getImps()[index].getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
        c.getSession().write(CWvsContext.updateImp(c.getPlayer().getImps()[index], ImpFlag.REMOVED.getValue(), index, false));
        c.getPlayer().getImps()[index] = null;
    }
}