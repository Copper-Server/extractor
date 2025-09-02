package copper_server.extractor

import net.minecraft.server.MinecraftServer
import com.google.gson.JsonElement

interface Extractor{
    @Throws(Exception::class)
    fun extract(server: MinecraftServer): JsonElement
}