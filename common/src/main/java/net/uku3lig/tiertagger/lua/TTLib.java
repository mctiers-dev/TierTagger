package net.uku3lig.tiertagger.lua;

import com.google.gson.*;
import net.uku3lig.tiertagger.TierTagger;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class TTLib extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = new LuaTable();
        library.set("fetch", new Fetch());
        library.set("parse_json", new ParseJson());

        // global var
        env.set("tiertagger", library);
        // for usage in require()
        if (!env.get("package").isnil()) {
            env.get("package").get("loaded").set("tiertagger", library);
        }

        return library;
    }

    static class Fetch extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(arg.checkjstring())).build();
            try {
                HttpResponse<String> res = TierTagger.getClient().send(request, HttpResponse.BodyHandlers.ofString());
                return LuaValue.valueOf(res.body());
            } catch (Exception e) {
                return NIL;
            }
        }
    }

    static class ParseJson extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            JsonElement root = TierTagger.GSON.fromJson(arg.checkjstring(), JsonElement.class);
            return jsonToLua(root);
        }
    }

    private static LuaValue jsonToLua(JsonElement json) {
        if (json.isJsonNull()) {
            return LuaValue.NIL;
        } else if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return LuaValue.valueOf(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                return LuaValue.valueOf(primitive.getAsDouble());
            } else {
                return LuaValue.valueOf(primitive.getAsString());
            }
        } else if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            LuaTable table = new LuaTable();
            for (int i = 0; i < arr.size(); i++) {
                table.set(i, jsonToLua(arr.get(i)));
            }
            return table;
        } else if (json.isJsonObject()) {
            LuaTable table = new LuaTable();
            json.getAsJsonObject().asMap().forEach((k, v) -> table.set(k, jsonToLua(v)));
            return table;
        } else {
            throw new UnsupportedOperationException("unknown json type");
        }
    }

    public static JsonElement luaToJson(LuaValue lua) {
        if (lua.isnil()) {
            return JsonNull.INSTANCE;
        } else if (lua.isboolean()) {
            return new JsonPrimitive(lua.toboolean());
        } else if (lua.isnumber()) {
            return new JsonPrimitive(lua.todouble());
        } else if (lua.isstring()) {
            return new JsonPrimitive(lua.tojstring());
        } else if (lua.istable()) {
            LuaTable table = lua.checktable();
            boolean isArray = Arrays.stream(table.keys()).allMatch(LuaValue::isint);
            if (isArray) {
                JsonArray array = new JsonArray(table.keyCount());
                Arrays.stream(table.keys()).mapToInt(LuaValue::toint).sorted().forEach(i -> array.add(luaToJson(table.get(i))));
                return array;
            } else {
                JsonObject object = new JsonObject();
                Arrays.stream(table.keys()).forEach(k -> {
                    if (k.isstring()) {
                        object.add(k.tojstring(), luaToJson(table.get(k)));
                    } else {
                        TierTagger.getLogger().warn("invalid lua table key: {}", k.checkjstring());
                    }
                });
                return object;
            }
        } else {
            throw new UnsupportedOperationException("invalid lua value: " + lua.checkjstring());
        }
    }
}
