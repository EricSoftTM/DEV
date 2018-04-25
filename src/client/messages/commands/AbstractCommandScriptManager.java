/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleClient;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import tools.FileoutputUtil;

/**
 *
 * @author Alex
 */
    public class AbstractCommandScriptManager {
        
        private final Map<MapleClient, AbstractCommandScriptManager> cms = new WeakHashMap<MapleClient, AbstractCommandScriptManager>();
        private static final ScriptEngineManager sem = new ScriptEngineManager();
        private static AbstractCommandScriptManager instance = new AbstractCommandScriptManager();
        
        public void putCms(MapleClient c, AbstractCommandScriptManager acm) {
            cms.put(c, acm);
        }
        
        public static AbstractCommandScriptManager getInstance() {
            return instance;
        }
        
        public final void dispose(final MapleClient c, final String commandType, final String name) {
            final AbstractCommandScriptManager npccm = cms.get(c);
            if (npccm != null) {
                cms.remove(c);
                c.removeScriptEngine("scripts/commands/" + commandType + "/" + name + ".js");
                c.removeScriptEngine("scripts/commands/nocommand.js");
            }
            if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
                c.getPlayer().setConversation(0);
            }
        }
        
        public static Invocable getInvocableCommand(String commandType, String path, MapleClient c) {
            FileReader fr = null;
            try {
                if (!path.equals("nocommand")) {
                    path = "scripts/commands/" + commandType + "/" + path + ".js";
                } else {
                    path = "scripts/commands/nocommand.js";
                }
                ScriptEngine engine = null;
     
                if (c != null) {
                    engine = c.getScriptEngine(path);
                }
                if (engine == null) {
                    File scriptFile = new File(path);
                    if (!scriptFile.exists()) {
                        return null;
                    }
                    engine = sem.getEngineByName("javascript");
                    if (c != null) {
                        c.setScriptEngine(path, engine);
                    }
                    fr = new FileReader(scriptFile);
                    engine.eval(fr);
                }
                return (Invocable) engine;
            } catch (Exception e) {
                System.err.println("Error executing script. Path: " + commandType + "/" + path + "\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing script. Path: " + commandType + "/" + path + "\nException " + e);
                return null;
            } finally {
                try {
                    if (fr != null) {
                        fr.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }
    }
