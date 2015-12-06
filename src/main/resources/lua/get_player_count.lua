local c = redis.call

local curTime = c("TIME")
local time = tonumber(curTime[1])

local heartbeats = c("HGETALL", "heartbeats")
local total = 0
local key

for _, v in ipairs(heartbeats) do
    if not key then
        key = v
    else
        local n = tonumber(v)
        if n then
            if n + 30 >= time then
                total = total + c("SCARD", "proxy:" .. key .. ":usersOnline")
            end
        end
        key = nil
    end
end

return total
