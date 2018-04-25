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
package server.maps;

import client.MapleCharacter;
import client.MapleCharacter.DojoMode;
import client.MapleTrait.MapleTraitType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Point;
import server.Randomizer;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.OverrideMonsterStats;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.packet.CWvsContext;

public class Event_DojoAgent {

    private final static int baseAgentMapId = 970030000; // 9500337 = mano
    private final static Point point1 = new Point(140, 0),
            point2 = new Point(-193, 0),
            point3 = new Point(355, 0);
    private static int dojoMode = -1;

    public static boolean warpStartAgent(final MapleCharacter c, final boolean party) {
        final int stage = 1;
        final int mapid = baseAgentMapId + (stage * 100);

        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = mapid; i < mapid + 15; i++) {
            final MapleMap map = ch.getMapFactory().getMap(i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));
                map.respawn(true);
                return true;
            }
        }
        return false;
    }

    public static boolean warpNextMap_Agent(final MapleCharacter c, final boolean fromResting) {
        final int currentmap = c.getMapId();
        final int thisStage = (currentmap - baseAgentMapId) / 100;

        MapleMap map = c.getMap();
        if (map.getSpawnedMonstersOnMap() > 0) {
            return false;
        }
        if (!fromResting) {
            clearMap(map, true);
        }
        final ChannelServer ch = c.getClient().getChannelServer();
        if (currentmap >= 970032700 && currentmap <= 970032800) {
            map = ch.getMapFactory().getMap(baseAgentMapId);
            c.changeMap(map, map.getPortal(0));
            return true;
        }
        final int nextmapid = baseAgentMapId + ((thisStage + 1) * 100);
        for (int i = nextmapid; i < nextmapid + 7; i++) {
            map = ch.getMapFactory().getMap(i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));
                map.respawn(true);
                return true;
            }
        }
        return false;
    }

    public static boolean warpStartDojo(final MapleCharacter c, final boolean party) {
        int stage = 1;
        if (party || stage <= -1 || stage > 38) {
            stage = 1;
        }
        int mapid = 925020000 + (stage * 100);
        boolean canenter = false;
        final ChannelServer ch = c.getClient().getChannelServer();
        for (int x = 0; x < 10; x++) { //15 maps each stage
            boolean canenterr = true;
            for (int i = 1; i < 39; i++) { //only 32 stages, but 38 maps
                MapleMap map = ch.getMapFactory().getMap(925020000 + 100 * i + x);
                if (map.getCharactersSize() > 0) {
                    canenterr = false;
                    break;
                } else {
                    clearMap(map, false);
                }
            }
            if (canenterr) {
                canenter = true;
                mapid += x;
                break;
            }
        }
        final MapleMap map = ch.getMapFactory().getMap(mapid);
        final MapleMap mapidd = c.getMap();
        if (canenter) {
            if (party && c.getParty() != null) {
                for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                    MapleCharacter chr = mapidd.getCharacterById(mem.getId());
                    if (chr != null && chr.isAlive()) {
                        chr.changeMap(map, map.getPortal(0));
                    }
                }
            } else {
                c.changeMap(map, map.getPortal(0));
            }
            switch (c.getDojoMode()) {
                case EASY:
                    dojoMode = 0;
                    break;
                case NORMAL:
                    dojoMode = 1;
                    break;
                case HARD:
                    dojoMode = 2;
                    break;
                case RANKED:
                    dojoMode = 3;
                    break;
                default: // DojoMode.NONE
                    return false;
            }
            spawnMonster(map, stage, dojoMode);
        }
        return canenter;
    }

    public static void failed(final MapleCharacter c) {
        final MapleMap currentmap = c.getMap();
        final MapleMap deadMap = c.getClient().getChannelServer().getMapFactory().getMap(925020002);
        if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
            for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                if (chr != null) {
                    chr.changeMap(deadMap, deadMap.getPortal(0));
                }
            }
        }
    }

    // Resting rooms :
    // 925020600 ~ 925020609
    // 925021200 ~ 925021209
    // 925021800 ~ 925021809
    // 925022400 ~ 925022409
    // 925023000 ~ 925023009
    // 925023600 ~ 925023609
    public static boolean warpNextMap(final MapleCharacter c, final boolean fromResting, final MapleMap currentmap) {
        try {
            final int temp = (currentmap.getId() - 925000000) / 100;
            final int thisStage = (int) (temp - ((temp / 100) * 100));
            final int points = getDojoPoints(thisStage);
            final ChannelServer ch = c.getClient().getChannelServer();
            final MapleMap deadMap = ch.getMapFactory().getMap(925020002);
            if (!c.isAlive()) { //shouldn't happen
                c.changeMap(deadMap, deadMap.getPortal(0));
                return true;
            }
            final MapleMap map = ch.getMapFactory().getMap(currentmap.getId() + 100);
            if (!fromResting && map != null) {
                clearMap(currentmap, true);
                if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            c.getTrait(MapleTraitType.will).addExp(points, c);
                            final int dojo = chr.getIntRecord(GameConstants.DOJO) + points;
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                            // chr.getClient().getSession().write(CWvsContext.Mulung_Pts(points, dojo));
                        }
                    }
                } else {
                    c.getTrait(MapleTraitType.will).addExp(points, c);
                    final int dojo = c.getIntRecord(GameConstants.DOJO) + points;
                    c.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                    // c.getClient().getSession().write(CWvsContext.Mulung_Pts(points, dojo));
                }
            }
            if (currentmap.getId() >= 925023800 && currentmap.getId() <= 925023814) {
                final MapleMap lastMap = ch.getMapFactory().getMap(925020003);

                if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            if (!chr.isAlive()) {
                                chr.addHP(50);
                            }
                            chr.changeMap(lastMap, lastMap.getPortal(1));
                            int exp = chr.getLevel() * (2000 * Randomizer.rand(10, 50));
                            c.dropMessage(5, "Mu Lung Dojo " + 
                            (c.getDojoMode() == DojoMode.EASY ? "Easy Mode" : 
                                    c.getDojoMode() == DojoMode.NORMAL ? "Normal Mode" : 
                                            c.getDojoMode() == DojoMode.HARD ? "Hard Mode" : 
                                                    c.getDojoMode() == DojoMode.RANKED ? "Ranked Mode" : "Unknown Mode") 
                            + " cleared! You've earned " + exp + " Exp!");
                            final int point = (points * 2);
                            c.getTrait(MapleTraitType.will).addExp(points, c);
                            final int dojo = chr.getIntRecord(GameConstants.DOJO) + point;
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                            // chr.getClient().getSession().write(CWvsContext.Mulung_Pts(point, dojo));
                        }
                    }
                } else {
                    c.changeMap(lastMap, lastMap.getPortal(1));
                    // c.startMapEffect("You have mastered the Mu Lung Dojo. Congratulations!", 5120000);
                    int exp = c.getLevel() * (2000 * Randomizer.rand(10, 50));
                    c.dropMessage(5, "Mu Lung Dojo " + 
                            (c.getDojoMode() == DojoMode.EASY ? "Easy Mode" : 
                                    c.getDojoMode() == DojoMode.NORMAL ? "Normal Mode" : 
                                            c.getDojoMode() == DojoMode.HARD ? "Hard Mode" : 
                                                    c.getDojoMode() == DojoMode.RANKED ? "Ranked Mode" : "Unknown Mode") 
                            + " cleared! You've earned " + exp + " Exp!");
                    if (c.dojoStartTime == 1337) {
                        World.Broadcast.broadcastMessage(c.getWorld(), CWvsContext.serverNotice(6, "[Mu Lung Dojo] " + c.getName() + " has achieved the best record in Mu Lung Dojo Ranked Mode."));
                    }
                    final int point = (points * 3);
                    c.getTrait(MapleTraitType.will).addExp(points, c);
                    final int dojo = c.getIntRecord(GameConstants.DOJO) + point;
                    c.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                    // c.getClient().getSession().write(CWvsContext.Mulung_Pts(point, dojo));
                }
                return true;
            }

            //final int nextmapid = 925020000 + ((thisStage + 1) * 100);
            if (map != null && map.getCharactersSize() == 0) {
                clearMap(map, false);
                if (c.getParty() != null) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            if (!chr.isAlive()) {
                                chr.addHP(50);
                            }
                            chr.changeMap(map, map.getPortal(0));
                        }
                    }
                } else {
                    c.changeMap(map, map.getPortal(0));
                }
                spawnMonster(map, thisStage + 1, dojoMode);
                return true;
            } else if (map != null) { //wtf, find a new map
                int basemap = currentmap.getId() / 100 * 100 + 100;
                for (int x = 0; x < 10; x++) {
                    MapleMap mapz = ch.getMapFactory().getMap(basemap + x);
                    if (mapz.getCharactersSize() == 0) {
                        clearMap(mapz, false);
                        if (c.getParty() != null) {
                            for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                                MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                                if (chr != null) {
                                    if (!chr.isAlive()) {
                                        chr.addHP(50);
                                    }
                                    chr.changeMap(mapz, mapz.getPortal(0));
                                }
                            }
                        } else {
                            c.changeMap(mapz, mapz.getPortal(0));
                        }
                        spawnMonster(mapz, thisStage + 1, dojoMode);
                        return true;
                    }
                }
            }
            final MapleMap mappz = ch.getMapFactory().getMap(925020001);
            if (c.getParty() != null) {
                for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                    MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                    if (chr != null) {
                        chr.dropMessage(5, "An error has occurred and you shall be brought to the beginning.");
                        chr.changeMap(mappz, mappz.getPortal(0));
                    }
                }
            } else {
                c.dropMessage(5, "An error has occurred and you shall be brought to the beginning.");
                c.changeMap(mappz, mappz.getPortal(0));
            }
        } catch (Exception rm) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, rm);
        }

        return false;
    }

    private static void clearMap(final MapleMap map, final boolean check) {
        if (check) {
            if (map.getCharactersSize() != 0) {
                return;
            }
        }
        map.resetFully();
    }

    private static int getDojoPoints(final int stage) {
        switch (stage) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return 2;
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                return 3;
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                return 4;
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return 5;
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
                return 6;
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                return 7;
            case 37:
            case 38:
                return 8;
            default:
                return 0;
        }
    }

    private static void spawnMonster(final MapleMap map, final int stage, final int dojoMode) {
        final int mobid;
        switch (stage) {
            case 1:
                mobid = 9300184; // Mano
                break;
            case 2:
                mobid = 9300185; // Stumpy
                break;
            case 3:
                mobid = 9300186; // Dewu
                break;
            case 4:
                mobid = 9300187; // King Slime
                break;
            case 5:
                mobid = 9300188; // Giant Centipede
                break;
            case 7:
                mobid = 9300189; // Faust
                break;
            case 8:
                mobid = 9300190; // King Clang
                break;
            case 9:
                mobid = 9300191; // Mushmom
                break;
            case 10:
                mobid = 9300192; // Alishar
                break;
            case 11:
                mobid = 9300193; // Timer
                break;
            case 13:
                mobid = 9300194; // Dale
                break;
            case 14:
                mobid = 9300195; // Papa Pixie
                break;
            case 15:
                mobid = 9300196; // Zombie Mushmom
                break;
            case 16:
                mobid = 9300197; // Jeno
                break;
            case 17:
                mobid = 9300198; // Lord Pirate
                break;
            case 19:
                mobid = 9300199; // Old Fox
                break;
            case 20:
                mobid = 9300200; // Tae Roon
                break;
            case 21:
                mobid = 9300201; // Poison Golem
                break;
            case 22:
                mobid = 9300202; // Ghost Priest
                break;
            case 23:
                mobid = 9300203; // Jr. Balrog
                break;
            case 25:
                mobid = 9300204; // Eliza
                break;
            case 26:
                mobid = 9300205; // Frankenroid
                break;
            case 27:
                mobid = 9300206; // Chimera
                break;
            case 28:
                mobid = 9300207; // Snack Bar
                break;
            case 29:
                mobid = 9300208; // Snowman
                break;
            case 31:
                mobid = 9300209; // Blue Mushmom
                break;
            case 32:
                mobid = 9300210; // Crimson Balrog
                break;
            case 33:
                mobid = 9300211; // Manon
                break;
            case 34:
                mobid = 9300212; // Griffey
                break;
            case 35:
                mobid = 9300213; // Leviathan
                break;
            case 37:
                mobid = 9300214; // Papulatus
                break;
            case 38:
                mobid = 9300215; // Mu gong
                break;
            default:
                return;
        }
        if (mobid != 0) {
            final int rand = Randomizer.nextInt(3);

            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    MapleMonster mob = MapleLifeFactory.getMonster(mobid);
                    OverrideMonsterStats dojoStats = new OverrideMonsterStats();
                    switch(dojoMode) {
                        case 0:
                            dojoStats.setOHp(mob.getMobMaxHp() / 2); // decrease their hp, have same attack
                            dojoStats.setOMp(mob.getMobMaxMp());
                            dojoStats.setOExp(mob.getMobExp() / 2);
                            break;
                        case 1:
                            dojoStats.setOHp(mob.getMobMaxHp() * 2); // normal - keep it the way it is
                            dojoStats.setOMp(mob.getMobMaxMp() * 2);
                            dojoStats.setOExp(mob.getMobExp());
                            break;
                        case 2:
                            dojoStats.setOHp(mob.getMobMaxHp() * 8); // 3x their hp
                            dojoStats.setOMp(mob.getMobMaxMp() * 8); // 3x their attack
                            dojoStats.setOExp(mob.getMobExp() * 2);
                            break;
                        case 3:
                            dojoStats.setOHp(mob.getMobMaxHp() * 15); // 5x their hp and timed
                            dojoStats.setOMp(mob.getMobMaxMp() * 15); // 5 their attack and timed
                            dojoStats.setOExp(mob.getMobExp() * 3); // ehh..
                            break;
                        default: // DojoMode.NONE
                            return;
                    }
                    mob.setOverrideStats(dojoStats);
                    map.spawnMonsterWithEffect(mob, 15, rand == 0 ? point1 : rand == 1 ? point2 : point3);
                }
            }, 3000);
        }
    }
}
