-- This script needs all active proxies available specified as args.
local insert = table.insert
local call = redis.call

local serverToData = {}

for _, proxy in ipairs(ARGV) do
    local players = call("SMEMBERS", "proxy:" .. proxy .. ":usersOnline")
    for _, player in ipairs(players) do
        local server = call("HGET", "player:" .. player, "server")
        if server then
            if not serverToData[server] then
                serverToData[server] = {}
            end
            insert(serverToData[server], player)
        end
    end
end

-- Redis can't map a Lua table back, so we have to send it as JSON.
return cjson.encode(serverToData)