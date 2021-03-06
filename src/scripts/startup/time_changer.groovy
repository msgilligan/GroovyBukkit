import net.krsmes.bukkit.groovy.GroovyRunner

command 't', { GroovyRunner r, List args ->
    if (args) switch (args.remove(0)) {
        case 'sunrise':
            r.world.time = 22000
            return "Sunrise of day ${Math.round(w.fullTime / 24000)}"

        case 'morning':
            r.world.time = 0
            return "Morning of day ${Math.round(w.fullTime / 24000)}"

        case 'noon':
            r.world.time = 6000
            return "Noon of day ${Math.round(w.fullTime / 24000)}"

        case 'sunset':
            r.world.time = 12000
            return "Sunset of day ${Math.round(w.fullTime / 24000)}"

        case 'night':
            r.world.time = 14000
            return "Night of day ${Math.round(w.fullTime / 24000)}"

        case 'alwaysday':
            if (!args) return "Error: Specify 'on' or 'off'"
            if (args?.remove(0)?.toLowerCase() == 'off')
                r.unlisten 'alwaysday'
            else {
                r.listen 'alwaysday', 'hour change', { if (it.hour == 12) it.world.time = 0 }
                r.world.time = 0
                return "Morning of day ${Math.round(r.world.fullTime / 24000)}"
            }
    }
    "/t NAME : sunrise, morning, noon, sunset, night, alwaysday"
}


