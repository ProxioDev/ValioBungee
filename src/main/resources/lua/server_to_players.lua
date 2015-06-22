local call = redis.call
local ipairs = ipairs

local serverToData = {}

for _, proxy in ipairs(ARGV) do
    local players = call("SMEMBERS", "proxy:" .. proxy .. ":usersOnline")
    for _, player in ipairs(players) do
        local server = call("HGET", "player:" .. player, "server")
        if server then
            local sz = #serverToData
            serverToData[sz + 1] = server
            serverToData[sz + 2] = player
        end
    end
end

return serverToData