package copper_server.extractor

import com.google.gson.GsonBuilder
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import copper_server.extractor.extractors.*
import dev.turingcomplete.textcaseconverter.*

class Entry : ModInitializer {
    private val modID: String = "copper_server extractor"
    private val logger: Logger = LoggerFactory.getLogger(modID)

    override fun onInitialize() {
        val extractors = arrayOf(
            BlockEntityMap(),
            BlockEntityTypes(),
            BlockProperties(),
            ComposterIncreaseChance(),
            Entities(),
            EntityAttributes(),
            EntityPose(),
            FlowerPotTransformation(),
            Fuels(),
            Items(),
            Potion(),
            PotionRecipes(),
            Recipes(),
            SpawnEgg(),
            StatusEffects(),
            SyncedRegistries(),
            Protocol(),
        )
        val multiExtractors = arrayOf(
            Blocks()
        )

        val outputDirectory: Path
        try {
            outputDirectory = Files.createDirectories(Paths.get("output"))
        } catch (e: IOException) {
            logger.info("Failed to create output directory.", e)
            return
        }

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server: MinecraftServer ->
            for (ext in extractors) {
                var fileName = StandardTextCases.SNAKE_CASE.convertFrom(StandardTextCases.STRICT_CAMEL_CASE, ext.javaClass.simpleName) + ".json"
                try {
                    val out = outputDirectory.resolve(fileName)
                    val fileWriter = FileWriter(out.toFile(), StandardCharsets.UTF_8)
                    gson.toJson(ext.extract(server), fileWriter)
                    fileWriter.close()
                    logger.info("Wrote " + out.toAbsolutePath())
                } catch (e: Exception) {
                    logger.error("Failed to extract ${fileName}.", e)
                }
            }
            for (ext in multiExtractors) {
                for(file in ext.extract(server)) {
                    try {
                        val out = outputDirectory.resolve(file.second)
                        val fileWriter = FileWriter(out.toFile(), StandardCharsets.UTF_8)
                        gson.toJson(file.first, fileWriter)
                        fileWriter.close()
                        logger.info("Wrote " + out.toAbsolutePath())
                    } catch (e: Exception) {
                        logger.error("Failed to extract ${file.second}.", e)
                    }
                }
            }
            server.stop(false)
        })
    }
}
