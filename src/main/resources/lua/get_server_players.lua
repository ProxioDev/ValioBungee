local c = redis.call
local u = string.upper

local curTime = c("TIME")
local time = tonumber(curTime[1])

local heartbeats = c("HGETALL", "heartbeats")
local all = {}
local key

local preUppercased = u(ARGV[1])

for _, v in ipairs(heartbeats) do
    if not key then
        key = v
    else
        local n = tonumber(v)
        if n then
            if n + 30 >= time then
                local players = c("SMEMBERS", "proxy:" .. key .. ":usersOnline")
                for _, player in ipairs(players) do
                    local server = c("HGET", "player:" .. player, "server")
                    if server and u(server) == preUppercased then
                        all[#all + 1] = player
                    end
                end
            end
        end
        key = nil
    end
end

return all
