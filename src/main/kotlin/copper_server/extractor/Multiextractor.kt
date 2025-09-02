package copper_server.extractor

import net.minecraft.server.MinecraftServer
import com.google.gson.JsonElement

interface MultiExtractor{
    @Throws(Exception::class)
    fun extract(server: MinecraftServer): Array<Pair<JsonElement, String>>
}