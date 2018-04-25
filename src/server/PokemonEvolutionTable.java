/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author FateJiki
 */
public class PokemonEvolutionTable {
    private enum Evolutions{
        FIRST(25),
        SECOND(130),
        THIRD(250),
        FOURTH(400),
        ;
        
        final int reqLv;
        private Evolutions(int reqid_){
            reqLv = reqid_;
        }
        
        public static Evolutions getEvolutionById(int lv){
            for(Evolutions e : Evolutions.values()){
                if(e.reqLv == lv){
                    return e;
                }
            }
            
            return null;
        }
        
    }
        public static boolean doesEvolve(int lv){
            return Evolutions.getEvolutionById(lv) != null;
        }
}
