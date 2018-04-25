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
package client;

public class InnerSkillValueHolder {
    
    private int skillId = 0;
    private byte skillLevel = 0;
    private byte maxLevel = 0;
    private byte rank = 0;
    public String skillName = "";
    public InnerSkillValueHolder(int skillId, byte skillLevel, byte maxLevel, byte rank){
        this.skillId = skillId;
        this.skillLevel = skillLevel;
        this.maxLevel = maxLevel;
        this.rank = rank;
    }
    
    public int getSkillId(){
        return skillId;
    }
    
    public byte getSkillLevel(){
        return skillLevel;
    }
    
    public byte getMaxLevel(){
        return maxLevel;
    }
    
    public byte getRank(){
        return rank;
    }
    //For reference's sake. 
    public String getSkillName(){
        switch(getSkillId()){
            case 70000000: skillName = "STR +#"; break; //strFX = x
            case 70000001: skillName = "DEX +#"; break; //dexFX = x
            case 70000002: skillName = "INT +#"; break; //intFX = x
            case 70000003: skillName = "LUK +#"; break; //lukFX = x
            case 70000004: skillName = "Accuracy +#"; break; //accX = 10 * x
            case 70000005: skillName = "Avoidance +#"; break; //evaX = 10 * x
            case 70000006: skillName = "Weapon DEF +#"; break; //pddX = 10 * x
            case 70000007: skillName = "Magic DEF +#"; break; //mddX = 10 * x
            case 70000008: skillName = "Max HP +#"; break; //mhpX = x * 15
            case 70000009: skillName = "Max MP +#"; break; //mmpX = x * 15
            case 70000010: skillName = "Jump +#"; break; //psdJump = 2 * u (x / 3)
            case 70000011: skillName = "Movment Speed +#"; break; //psdSpeed ??= 2 * u (x / 3)
            case 70000012: skillName = "Attack +#"; break; //padX = 3 * u (x / 3)
            case 70000013: skillName = "Magic Attack +#"; break; //madX = 3 * u (x / 3)
            case 70000014: skillName = "Critical Rate +#"; break; //cr = x
            case 70000015: skillName = "All Stats +#"; break; //lukFX = x	strFX = x	dexFX = x	intFX = x
            case 70000016: skillName = "Attack Speed +"; break; // actionSpeed ??= -1
            case 70000017: skillName = "% Wep DEF to Magic DEF"; break; // pdd2mdd = u (x / 4)
            case 70000018: skillName = "% Magic DEF to Wep DEF"; break; //mdd2pdd = u (x / 4)
            case 70000019: skillName = "% Acc to Max MP"; break; //acc2mp = 5 * u (x / 4)
            case 70000020: skillName = "% Avoid to Max HP"; break; //eva2hp = 5 * u (x / 4)
            case 70000021: skillName = "% STR to DEX"; break; //str2dex = u (x / 4)
            case 70000022: skillName = "% DEX to STR"; break; //dex2str = u (x / 4)
            case 70000023: skillName = "% INT to LUK"; break; //int2luk = u (x / 4)	
            case 70000024: skillName = "% LUK to DEX"; break; //luk2dex = u (x / 4)	
            case 70000025: skillName = "Attack +1 for every X levels"; break; //lv2pad = 20-2 * d (x / 2)	
            case 70000026: skillName = "Magic Attack +1 for every X levels"; break; //lv2mad = 20-2 * d (x / 2)	
            case 70000027: skillName = "Acc +%"; break; //accR = x
            case 70000028: skillName = "Avoid +%"; break; //evaR = x
            case 70000029: skillName = "Weapon DEF +%"; break; //pddR = x
            case 70000030: skillName = "Magic DEF +%"; break; //mddR = x
            case 70000031: skillName = "Max HP +%"; break; //mhpR = x
            case 70000032: skillName = "Max MP +%"; break; //mmpR =x
            case 70000033: skillName = "Acc Boost +%"; break; //ar = u (x/2)
            case 70000034: skillName = "Avoid Boost +%"; break; //er = u (x/2)
            case 70000035: skillName = "+% Damage to Bosses"; break; //bdR = x
            case 70000036: skillName = "+% Damage to Norm Mobs"; break;  //nbdR = u (x / 4)	
            case 70000037: skillName = "+% Damage to Towers"; break;  //tdR = 2 * u (x / 3)	
            case 70000038: skillName = "+% Chance to instant-kill when attacking normal mobs in Azwan Supply"; break; //minionDeathProp = u (x / 4)	
            case 70000039: skillName = "+% Damage when attacking targets inflicted with Stun, Blindness, or Freeze"; break; //abnormalDamR = u (x / 4)	
            case 70000040: skillName = "+% of Wep Acc or Magic Acc (>) added to additional damage"; break; //acc2dam = x * 2 + u (x / 2)	
            case 70000041: skillName = "+% Of Wep DEF added as additional damage"; break; //pdd2dam = x * 2 + u (x / 2)	
            case 70000042: skillName = "+% of Magic DEF added as additional damage"; break; //mdd2dam = x * 2 + u (x / 2)	
            case 70000043: skillName = "When hit with magic attack, damage equal to % of Wep DEF is ignored"; break; //pdd2mdx = u (x / 3)	
            case 70000044: skillName = "When hit with physical attack, damage equal to % of Magic DEF is ignored"; break; //mdd2pdx = u (x / 3)	
            case 70000045: skillName = "Cooldown is not applied at % Chance"; break; //nocoolProp = x	
            case 70000046: skillName = "Increase skill level of passive skills by #"; break; //passivePlus = 1	
            case 70000047: skillName = "Numbers of enemies hit by multi-target skill +#"; break; //targetPlus  = 1
            case 70000048: skillName = "Buff skill duraiton +%"; break;  //bufftimeR = x + u (x / 4)	
            case 70000049: skillName = "Item drop rate +%"; break; //dropR = u (x / 2)	
            case 70000050: skillName = "Mesos obtained +%"; break;  //mesoR = u (x / 2)	
            case 70000051: skillName = "STR +#, DEX +#"; break; //strFX = x	dexFX = u (x / 2)
            case 70000052: skillName = "STR +#, INT +#"; break; //strFX = x	intFX = u (x / 2)
            case 70000053: skillName = "STR +#, LUK +#"; break; //strFX = x	lukFX = u (x / 2)
            case 70000054: skillName = "DEX +#, INT +#"; break; //dexFX = x	intFX = u (x / 2)
            case 70000055: skillName = "DEX +#, LUK +#"; break; //dexFX = x	lukFX = u (x / 2)
            case 70000056: skillName = "INT +#, LUK +#"; break; //intFX = x	lukFX = u (x / 2)
            case 70000057: skillName = "DEX +#, STR +#"; break; //dexFX = x	strFX = u (x / 2)
            case 70000058: skillName = "INT +#, STR +#"; break; //intFX = x	strFX = u (x / 2)
            case 70000059: skillName = "LUK +#, STR +#"; break; //lukFX = x	strFX = u (x / 2)
            case 70000060: skillName = "INT +#, DEX +#"; break; //intFX = x	dexFX = u (x / 2)
            case 70000061: skillName = "LUK +#, DEX +#"; break; //lukFX = x	dexFX = u (x / 2)
            case 70000062: skillName = "LUK +#, INT +#"; break; //lukFX = x	intFX = u (x / 2)
        }
        return skillName;
    }
}