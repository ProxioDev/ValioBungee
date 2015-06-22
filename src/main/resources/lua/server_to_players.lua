-- This script needs all active proxies available specified as args.
local insert = table.insert
local call = redis.call

local serverToData = {}

for _, proxy in ipairs(ARGV) do
    local players = call("SMEMBERS", "proxy:" .. proxy .. ":usersOnline")
    for _, player in ipairs(players) do
        local server = call("HGET", "player:" .. player, "server")
        if server then
            local map = serverToData[server]
            if not map then
                serverToData[server] = {}
                map = serverToData[server]
            end
            insert(map, player)
        end
    end
end

-- Redis can't map Lua associative tables back, so we have to send it as JSON.
return cjson.encode(serverToData)