package ExpTable;

import java.util.ArrayList;
import java.util.List;

public class ExpTable {
    private static int maxLevel = 200;
    private static int start = 0;

    public static void main(String[] args) {
        int[] level = new int[maxLevel + 1]; 
        level[1] = 1;
        level[2] = 34;
        level[3] = 57;
        level[4] = 92;
        level[5] = 135;
        int level_fifteen = 15; // edi@1
        level[1] = 15;
        level[6] = 372;
        level[7] = 560;
        level[8] = 840;
        level[9] = 1242;
        int v1 = level[9]; // eax@1
        level[10] = v1;
        level[11] = v1;
        level[12] = v1;
        level[13] = v1;
        level[14] = v1;
        int level_thirtyfive = 35; // edi@3
        int v4 = 40; // edi@5
        int v6 = 75; // edi@7
        int v8 = 125; // edi@9
        int v9 = 160; // edi@11
        final List<Integer> levels = new ArrayList<>();
        for (int i = start; i < (maxLevel + 1); i++) { // lev+1 | 201
            if (i >= 0 && i <= 14) {
                levels.add(level[i]);
            } else if (i >= 15 && i <= 34) {
                if (i >= 30 && i <= 34) {
                    int v2 = level[29]; // eax@3
                    level[30] = v2;
                    level[31] = v2;
                    level[32] = v2;
                    level[33] = v2;
                    level[34] = v2;
                    levels.add(level[i]);
                } else {
                    level[level_fifteen] = (int)((double)level[level_fifteen - 1] * 1.2 + 0.5);
                    ++level_fifteen;
                    levels.add(level[i]);
                }
            } else if (i >= 35 && i <= 39) {
                level[level_thirtyfive] = (int)((double)level[level_thirtyfive - 1] * 1.2 + 0.5);
                ++level_thirtyfive;
                levels.add(level[i]);
            } else if (i >= 40 && i <= 74) {
                if (i >= 70 && i <= 74) {
                    int v5 = level[69]; // eax@7
                    level[70] = v5;
                    level[71] = v5;
                    level[72] = v5;
                    level[73] = v5;
                    level[74] = v5;
                    levels.add(level[i]);
                } else {
                    level[v4] = (int)((double)level[v4 - 1] * 1.08 + 0.5);
                    ++v4;
                    levels.add(level[i]);
                }
            } else if (i >= 75 && i <= 124) {
                if (i >= 120 && i <= 124) {
                    int v7 = level[119]; // eax@9
                    level[120] = v7;
                    level[121] = v7;
                    level[122] = v7;
                    level[123] = v7;
                    level[124] = v7;
                    levels.add(level[i]);
                } else {
                    level[v6] = (int)((double)level[v6 - 1] * 1.07 + 0.5);
                    ++v6;
                    levels.add(level[i]);
                }
            } else if (i >= 125 && i <= 159) {
                level[v8] = (int)((double)level[v8 - 1] * 1.07 + 0.5);
                ++v8;
                levels.add(level[i]);
            } else if (i >= 160 && i <= 199) {
                level[v9] = (int)((double)level[v9 - 1] * 1.06 + 0.5);
                ++v9;
                levels.add(level[i]);
            } else if (i == 200) {
                level[200] = 0;
                levels.add(level[i]);
            }
        }
        System.out.println(levels);
    }
}