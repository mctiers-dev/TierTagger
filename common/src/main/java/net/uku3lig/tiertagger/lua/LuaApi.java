package net.uku3lig.tiertagger.lua;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.uku3lig.tiertagger.TierTagger;
import net.uku3lig.tiertagger.model.GameMode;
import net.uku3lig.tiertagger.model.PlayerInfo;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LuaApi {
    private static LuaValue config = new LuaTable();

    private static Globals makeGlobals() {
        // restrict stdlib to stuff that is properly safe
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        globals.load(new JseMathLib());
        globals.load(new TTLib());
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }

    public static void loadScript() {
        String scriptStr = """
                local tt = require('tiertagger')
            
                return {
                    ['gamemodes'] = function()
                        local res = tt.fetch('https://mctiers.com/api/v2/mode/list')
                        local modes = tt.parse_json(res)
                        local table = {}
                        local i = 1
                        for k, v in pairs(modes) do
                            table[i] = { ["id"] = k, ["title"] = v["title"] }
                            i = i + 1
                        end
                        return table
                    end,
                    ['player'] = function(uuid)
                        local res = tt.fetch('https://mctiers.com/api/v2/profile/' .. uuid)
                        print(res)
                        return tt.parse_json(res)
                    end,
                    ["search"] = function(name)
                        local res = tt.fetch('https://mctiers.com/api/v2/profile/by-name' .. name)
                        return tt.parse_json(res)
                    end
                }
                """;

        LuaValue script = makeGlobals().load(scriptStr);
        // TODO validation
        config = script.call();
    }

    public static List<GameMode> getGameModes() {
        LuaValue modes = config.get("gamemodes").call();
        JsonElement json = TTLib.luaToJson(modes);
        return TierTagger.GSON.fromJson(json, new TypeToken<>() {});
    }

    public static PlayerInfo fetchPlayer(UUID uuid) {
        LuaValue modes = config.get("player").call(uuid.toString());
        JsonElement json = TTLib.luaToJson(modes);
        return TierTagger.GSON.fromJson(json, new TypeToken<>() {});
    }
}
