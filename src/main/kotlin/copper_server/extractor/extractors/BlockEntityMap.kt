package copper_server.extractor

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import net.minecraft.block.Block
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos

class BlockEntityMap : Extractor {
    private val uniqueTypes = mutableSetOf<String>()
    private val maxExpansionDepth = 4
    private val skipUnknownFields = true

    private val preserveUnknown = setOf("stateManager")

    private val nonPreservedFieldNames =
            setOf(
                    "components",
                    "world",
                    "pos",
                    "cachedState",
                    "removed",
                    "LOGGER",
                    "callback",
                    "REMAINDER_STACK",
                    "dataAttachments",
                    "syncedAttachments",
                    "attachedChangedListeners",
                    "lidAnimator",
                    "listenerData",
                    "vibrationCallback",
                    "vibrationListenerData",
                    "propertyDelegate",
                    "matchGetter",
            )

    private val nonPreservedFieldTypes =
            setOf(
                    "net.minecraft.world.World",
                    "net.minecraft.block.entity.ChestLidAnimator",
                    "org.slf4j.Logger",
                    "com.mojang.serialization.Codec",
                    "net.minecraft.block.entity.BlockEntityType",
            )

    private val fabricPrefix = "fabric_"

    private val baseNbtKeys = setOf("x", "y", "z", "id", "keepPacked")

    private val specialNbtMappings =
            mapOf(
                    "inventory" to "Items",
                    "heldStacks" to "Items",
                    "customName" to "CustomName",
                    "lock" to "lock",
                    "item" to "item",
                    "lootTable" to "LootTable",
                    "LootTableSeed" to "LootTableSeed",
                    "flowerPos" to "flower_pos",
                    "waxed" to "is_waxed",
                    "creakingPuppet" to "creaking",
                    "sherds" to "sherds",
                    "stack" to "item",
                    "itemsBeingCooked" to "Items",
                    "eventListener" to "cursors",
                    "vibrationListener" to "VibrationListener",
                    "lootTable" to "LootTable",
                    "lootTableSeed" to "LootTableSeed",
                    "targetEntity" to "Target",
                    "errors" to "errors",
                    "activationRange" to "activation_range",
                    "deactivationRange" to "deactivation_range",
                    "overrideLootTableToDisplay" to "override_loot_table_to_display",
                    "displayItem" to "display_item",
                    "stateUpdatingResumesAt" to "state_updating_resumes_at",
                    "itemsToEject" to "items_to_eject",
                    "connectedPlayers" to "connected_players",
                    "connectedParticlesRange" to "connected_particles_range",
                    "displayItem" to "display_item",
                    "triggered" to "triggered",
                    "errorMessage" to "error_message",
                    "test" to "test",
                    "recordStack" to "RecordItem",
                    "spawnDelay" to "Delay",
                    "ticksSinceSongStarted" to "ticks_since_song_started",
                    "noteBlockSound" to "note_block_sound"
            )

    private val targetetNbtMappings =
            mapOf(
                    "net.minecraft.block.entity.SkullBlockEntity" to
                            mapOf("customName" to "custom_name", "owner" to "profile"),
                    "net.minecraft.block.entity.MobSpawnerBlockEntity" to
                            mapOf(
                                    "spawnEntry" to "SpawnData",
                            ),
                    "net.minecraft.block.entity.PistonBlockEntity" to
                            mapOf(
                                    "pushedBlockState" to "blockState",
                                    "facing" to "facing",
                                    "extending" to "extending",
                                    "progress" to "progress",
                                    "source" to "source",
                            ),
            )

    private val expansionBlacklist =
            setOf(
                    "java.lang",
                    "java.util",
                    "net.minecraft.item",
                    "net.minecraft.text",
                    "net.minecraft.util.Identifier",
                    "net.minecraft.util.collection.DefaultedList",
                    "org.slf4j",
                    "com.mojang.datafixers",
                    "com.mojang.serialization",
                    "net.minecraft.util.DyeColor",
                    "net.minecraft.block.entity.SignText",
                    "net.minecraft.predicate.item.ItemPredicate",
                    "it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap",
                    "net.minecraft.block.entity.ViewerCountManager",
                    "net.minecraft.block.jukebox.JukeboxManager\$ChangeNotifier",
                    "net.minecraft.util.math.Direction",
                    "net.minecraft.block.BlockState",
                    "net.minecraft.util.math.Vec3i",
            )

    private val unnestingTypes =
            setOf(
                    "net.minecraft.block.spawner.MobSpawnerLogic",
                    "net.minecraft.world.event.Vibrations",
                    "net.minecraft.block.jukebox.JukeboxManager",
            )

    private val typesToGetSizeOf = setOf("net.minecraft.util.collection.DefaultedList")

    private fun extractEnumInfo(enumClass: Class<*>): JsonArray {
        val valuesArray = JsonArray()
        val constants = enumClass.enumConstants ?: return valuesArray

        val declaredFields =
                enumClass.declaredFields.filter { !it.isEnumConstant && !it.isSynthetic }

        for (constant in constants) {
            if (constant !is Enum<*>) continue
            val constantInfo = JsonObject()
            constantInfo.addProperty("name", constant.name)

            for (field in declaredFields) {
                try {
                    field.isAccessible = true
                    val value = field.get(constant)
                    when (value) {
                        is Number -> constantInfo.add(field.name, JsonPrimitive(value))
                        is String -> constantInfo.add(field.name, JsonPrimitive(value))
                        is Boolean -> constantInfo.add(field.name, JsonPrimitive(value))
                        else -> constantInfo.addProperty(field.name, value.toString())
                    }
                } catch (e: Exception) {}
            }
            valuesArray.add(constantInfo)
        }
        return valuesArray
    }

    private fun extractFieldsRecursively(
            instance: Any,
            associatedNbt: NbtCompound?,
            currentDepth: Int
    ): JsonArray {
        val fieldsJson = JsonArray()
        if (currentDepth > maxExpansionDepth) return fieldsJson

        val processedFieldNames = mutableSetOf<String>()
        var currentClass: Class<*>? = instance::class.java

        while (currentClass != null && currentClass != Object::class.java) {
            currentClass.declaredFields.forEach { field ->
                if (processedFieldNames.contains(field.name)) return@forEach
                processedFieldNames.add(field.name)

                if (Modifier.isStatic(field.modifiers)) return@forEach

                val nbtKey = findNbtKeyForField(field, associatedNbt)

                val fieldInfo = JsonObject()
                fieldInfo.addProperty("name", field.name)
                val fieldType = field.type
                fieldInfo.addProperty("type", sanitizeName(field.genericType.typeName))
                fieldInfo.addProperty("declaring_class", field.declaringClass.name)

                if (needToSkip(field)) return@forEach
                if (associatedNbt != null) {
                    val preservation = determinePreservation(field, associatedNbt, nbtKey)
                    if (preservation == NbtPreservation.NON_PRESERVED) return@forEach
                    if (skipUnknownFields)
                            if (nbtKey == "_____UNKNOWN_____")
                                    if (!preserveUnknown.contains(field.name))
                                        if(!shouldExpandType(fieldType))
                                            return@forEach

                    fieldInfo.addProperty("preservation", preservation.name)
                    fieldInfo.addProperty("nbt_name", nbtKey)
                } else if (nbtKey != "_____UNKNOWN_____") {
                    fieldInfo.addProperty("preservation", NbtPreservation.UNKNOWN.name)
                    fieldInfo.addProperty("nbt_name", nbtKey)
                } else if (skipUnknownFields) {
                    if (!preserveUnknown.contains(field.name)) 
                        if(!shouldExpandType(fieldType))
                            return@forEach
                }

                uniqueTypes.add(sanitizeName(field.genericType.typeName))

                try {
                    field.isAccessible = true
                    val fieldValue = field.get(instance)

                    if (currentDepth == 1 && typesToGetSizeOf.contains(fieldType.name)) {
                        (fieldValue as? Collection<*>)?.let {
                            fieldInfo.addProperty("default_size", it.size)
                        }
                    }

                    (fieldValue as? IntArray)?.let {
                        fieldInfo.addProperty("size", it.size)
                    }
                    (fieldValue as? DoubleArray)?.let {
                        fieldInfo.addProperty("size", it.size)
                    }
                    (fieldValue as? LongArray)?.let {
                        fieldInfo.addProperty("size", it.size)
                    }
                    (fieldValue as? ByteArray)?.let {
                        fieldInfo.addProperty("size", it.size)
                    }

                    if (fieldValue != null && shouldUnnestType(fieldType)) {
                        val nestedFields =
                                extractFieldsRecursively(fieldValue, associatedNbt, currentDepth)
                        if (nestedFields.size() > 0) {
                            fieldsJson.addAll(nestedFields)
                            return@forEach
                        }
                    }

                    if (fieldType.isEnum) {
                        fieldInfo.add("enum_values", extractEnumInfo(fieldType))
                        fieldInfo.addProperty("enum_value_sample", fieldValue.toString())
                    } else if (fieldValue != null && shouldExpandType(fieldType)) {
                        var dat: NbtCompound? = null

                        if (associatedNbt != null) {
                            if (associatedNbt.contains(nbtKey)) {
                                val elem = associatedNbt.getCompound(nbtKey)
                                if (elem.isPresent) dat = elem.get()
                            }
                        }

                        val nestedFields =
                                extractFieldsRecursively(fieldValue, dat, currentDepth + 1)
                        if (nestedFields.size() > 0) {
                            fieldInfo.add("nested_fields", nestedFields)
                        }
                    }
                } catch (e: Exception) {}

                if (skipUnknownFields)
                    if (nbtKey == "_____UNKNOWN_____")//hide expanded type
                            if (!preserveUnknown.contains(field.name))
                                    return@forEach

                fieldsJson.add(fieldInfo)
            }
            currentClass = currentClass.superclass
        }
        return fieldsJson
    }

    private val EXTRACTION_POS = BlockPos(0, 255, 0)
    override fun extract(server: MinecraftServer): JsonElement {
        val blockEntitiesJson = JsonObject()
        val registryLookup = server.registryManager
        val world = server.overworld

        for (blockEntityType in Registries.BLOCK_ENTITY_TYPE) {
            val blockEntityId =
                    Registries.BLOCK_ENTITY_TYPE.getId(blockEntityType)?.toString() ?: "unknown"
            val blockEntityInfo = JsonObject()

            try {
                val supportedBlock =
                        try {
                            val blocksField = blockEntityType::class.java.getDeclaredField("blocks")
                            blocksField.isAccessible = true
                            val blocksValue = blocksField.get(blockEntityType) as? Iterable<*>
                            (blocksValue?.firstOrNull() as? Block)
                        } catch (e: Exception) {
                            null
                        }
                if (supportedBlock == null) {
                    blockEntityInfo.addProperty(
                            "parsing_error",
                            "No specific blocks are associated with this BlockEntityType."
                    )
                    blockEntitiesJson.add(blockEntityId, blockEntityInfo)
                    continue
                }
                world.setBlockState(EXTRACTION_POS, supportedBlock.defaultState)

                var defaultNbt: NbtCompound? = null
                var blockEntityInstance = world.getBlockEntity(EXTRACTION_POS)
                if (blockEntityInstance == null) {
                    blockEntityInfo.addProperty(
                            "parsing_error",
                            "Could not instantiate BlockEntity."
                    )
                    blockEntitiesJson.add(blockEntityId, blockEntityInfo)
                    blockEntityInstance =
                            blockEntityType.instantiate(EXTRACTION_POS, supportedBlock.defaultState)
                } else defaultNbt = blockEntityInstance.createNbtWithIdentifyingData(registryLookup)

                val kClass = blockEntityInstance!!::class
                blockEntityInfo.addProperty("class", kClass.qualifiedName)
                blockEntityInfo.addProperty("id", Registries.BLOCK_ENTITY_TYPE.getRawId(blockEntityType))

                val fieldsJson = extractFieldsRecursively(blockEntityInstance, defaultNbt, 1)
                blockEntityInfo.add("fields", fieldsJson)
            } catch (e: Exception) {
                blockEntityInfo.addProperty(
                        "parsing_error",
                        "An exception occurred during extraction: ${e.message}\n${e.stackTraceToString()}"
                )
                val fields = blockEntityInfo.get("fields")?.asJsonArray
                fields?.forEach {
                    (it as JsonObject).addProperty("preservation", NbtPreservation.UNKNOWN.name)
                }
            }

            blockEntitiesJson.add(blockEntityId, blockEntityInfo)
        }
        blockEntitiesJson.add("data:discovered_types", JsonArray().apply {
            uniqueTypes.sorted().forEach { add(it) }
        })
        return blockEntitiesJson
    }

    private fun shouldExpandType(type: Class<*>): Boolean {
        if (type.isPrimitive) return false
        val typeName = type.name
        return expansionBlacklist.none { typeName.startsWith("$it") }
    }

    private fun shouldUnnestType(type: Class<*>): Boolean {
        if (type.isPrimitive) return false
        return unnestingTypes.contains(type.name)
    }

    private fun needToSkip(field: Field): Boolean {
        val mods = field.modifiers
        return Modifier.isStatic(mods) ||
                Modifier.isTransient(mods) ||
                nonPreservedFieldNames.contains(field.name) ||
                field.name.startsWith(fabricPrefix) ||
                nonPreservedFieldTypes.contains(field.type.name)
    }

    private fun determinePreservation(
            field: Field,
            defaultNbt: NbtCompound,
            nbtKey: String
    ): NbtPreservation {
        if (needToSkip(field)) {
            return NbtPreservation.NON_PRESERVED
        }

        if (!defaultNbt.contains(nbtKey)) {
            return NbtPreservation.OPTIONAL
        }

        val nbtValue = defaultNbt.get(nbtKey)
        return if (isNbtValueEmptyOrDefault(nbtValue)) {
            NbtPreservation.REQUIRED_DEFAULT_EMPTY
        } else {
            NbtPreservation.REQUIRED
        }
    }

    private fun findNbtKeyForField(field: Field, defaultNbt: NbtCompound?): String {
        val snake = camelToSnake(field.name)
        val pascal = field.name.replaceFirstChar { it.uppercaseChar() }
        if (defaultNbt != null) {
            if (defaultNbt.contains(snake)) return snake
            else if (defaultNbt.contains(pascal)) return pascal
        }
        if (targetetNbtMappings.containsKey(field.declaringClass.name)) {
            val mappings = targetetNbtMappings.getValue(field.declaringClass.name)
            if (mappings.containsKey(field.name)) return mappings.getValue(field.name)
        }
        if (specialNbtMappings.containsKey(field.name))
                return specialNbtMappings.getValue(field.name)
        return "_____UNKNOWN_____"
    }

    private fun isNbtValueEmptyOrDefault(element: NbtElement?): Boolean {
        return when (element) {
            null -> true
            is NbtList -> element.isEmpty()
            is NbtCompound -> element.isEmpty()
            is NbtString -> element.asString().isEmpty()
            else -> false
        }
    }

    private fun camelToSnake(str: String): String {
        return str.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase()
    }

    private fun sanitizeName(name: String): String {
        return name.replace("java.lang.", "")
                .replace("java.util.", "")
                .replace("org.joml.", "")
                .replace("net.minecraft.", "")
                .replace("com.mojang.", "")
                .replace("util.math.", "")
                .replace("text.", "")
                .replace("particle.", "")
                .replace("entity.", "")
                .replace("mob.", "")
                .replace("block.", "")
                .replace("passive.", "")
                .replace("village.", "")
                .replace("registry.entry.", "")
                .replace("registry.", "")
                .replace("item.", "")
                .replace("inventory.", "")
                .replace("recipe.", "")
                .replace("loot.", "")
                .replace("component.", "")
                .replace("structure.pool.", "")
                .replace("world.event.", "")
                .replace("vault.", "")
                .replace("decoration.painting.", "")
                .replace("util.collection.", "")
                .replace("util.", "")
                .replace("it.unimi.dsi.fastobjects.", "")
                .replace("nbt.", "")
                .replace("enums.", "")
                .replace("test.", "")
                .replace("datafixers.", "")
                .replace("type.", "")
    }
}
