/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
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
package client.messages;

import client.MapleClient;
import tools.packet.CWvsContext;

public class ServernoticeMapleClientMessageCallback implements MessageCallback {

    private MapleClient client;
    private int mode;

    public ServernoticeMapleClientMessageCallback(MapleClient client) {
        this(6, client);
    }

    public ServernoticeMapleClientMessageCallback(int mode, MapleClient client) {
        this.client = client;
        this.mode = mode;
    }

    @Override
    public void dropMessage(String message) {
        client.getSession().write(CWvsContext.serverNotice(mode, message));
    }
    
    @Override
    public void dropMessage(int type, String message) {
        client.getSession().write(CWvsContext.serverNotice(type, message));
    }
}
