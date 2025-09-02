package copper_server.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.Gson
import com.mojang.serialization.JsonOps
import copper_server.extractor.MultiExtractor
import net.minecraft.block.Block
import net.minecraft.block.ExperienceDroppingBlock
import net.minecraft.block.SideShapeType
import net.minecraft.loot.LootTable
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView
import java.util.*

class Blocks : MultiExtractor {
    companion object {
        private const val IS_AIR: Int = 1
        private const val IS_BURNABLE: Int = 2
        private const val IS_TOOL_REQUIRED: Int = 4
        private const val HAS_SIDED_TRANSPARENCY: Int = 8
        private const val IS_REPLACEABLE: Int = 16
        private const val IS_LIQUID: Int = 32
        private const val IS_SOLID: Int = 64
        private const val IS_FULL_CUBE: Int = 128
        private const val HAS_RANDOM_TICKS: Int = 256
        private const val HAS_COMPARATOR_OUTPUT: Int = 512

        private const val DOWN_SIDE_SOLID: Int = 0b00000001;
        private const val UP_SIDE_SOLID: Int = 0b00000010;
        private const val NORTH_SIDE_SOLID: Int = 0b00000100;
        private const val SOUTH_SIDE_SOLID: Int = 0b00001000;
        private const val WEST_SIDE_SOLID: Int = 0b00010000;
        private const val EAST_SIDE_SOLID: Int = 0b00100000;
        private const val DOWN_CENTER_SOLID: Int = 0b01000000;
        private const val UP_CENTER_SOLID: Int = 0b10000000;
    }

    private fun getFlammableData(): Map<Block, Pair<Int, Int>> {
        val flammableData = mutableMapOf<Block, Pair<Int, Int>>()
        val fireBlock = net.minecraft.block.Blocks.FIRE as net.minecraft.block.FireBlock;
        for (block in Registries.BLOCK) {
            val defaultState = block.defaultState
            val spreadChance = fireBlock.getSpreadChance(defaultState)
            val burnChance = fireBlock.getBurnChance(defaultState)
            if (spreadChance > 0 || burnChance > 0) {
                flammableData[block] = Pair(spreadChance, burnChance)
            }
        }

        return flammableData
    }

    override fun extract(server: MinecraftServer): Array<Pair<JsonElement, String>> {
        val blocksJson = JsonArray()

        val shapes: LinkedHashMap<Box, Int> = LinkedHashMap()

        val flammableData = getFlammableData()

        for (block in Registries.BLOCK) {
            val blockJson = JsonObject()
            
            blockJson.addProperty("id", Registries.BLOCK.getRawId(block))
            blockJson.addProperty("named_id", Registries.BLOCK.getId(block).toString())
            blockJson.addProperty("name", block.name.toString())
            blockJson.addProperty("translation_key", block.translationKey)
            blockJson.addProperty("slipperiness", block.slipperiness)
            blockJson.addProperty("velocity_multiplier", block.velocityMultiplier)
            blockJson.addProperty("jump_velocity_multiplier", block.jumpVelocityMultiplier)
            blockJson.addProperty("hardness", block.hardness)
            blockJson.addProperty("blast_resistance", block.blastResistance)
            blockJson.addProperty("item_id", Registries.ITEM.getRawId(block.asItem()))
            blockJson.addProperty("map_color_rgb", block.defaultMapColor.color)


            // Add flammable data if this block is flammable
            flammableData[block]?.let { (spreadChance, burnChance) ->
                val flammableJson = JsonObject()
                flammableJson.addProperty("spread_chance", spreadChance)
                flammableJson.addProperty("burn_chance", burnChance)
                blockJson.add("flammable", flammableJson)
            }

            if (block is ExperienceDroppingBlock) {
                blockJson.add(
                    "exp_drop", ExperienceDroppingBlock.CODEC.codec().encodeStart(
                        RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                        block,
                    ).getOrThrow()
                )
            }
            if (block.lootTableKey.isPresent) {
                val table = server.reloadableRegistries
                    .getLootTable(block.lootTableKey.get() as RegistryKey<LootTable?>)
                blockJson.add(
                    "loot_table", LootTable::CODEC.get().encodeStart(
                        RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                        table
                    ).getOrThrow()
                )
            }
            val propsJson = JsonArray()
            for (prop in block.stateManager.properties) 
                propsJson.add(prop.hashCode())
            blockJson.add("properties", propsJson)

            val statesJson = JsonArray()
            for (state in block.stateManager.states) {
                val stateJson = JsonObject()
                var stateFlags = 0
                var sideFlags = 0
                
                if (state.isAir) stateFlags = stateFlags or IS_AIR
                if (state.isBurnable) stateFlags = stateFlags or IS_BURNABLE
                if (state.isToolRequired) stateFlags = stateFlags or IS_TOOL_REQUIRED
                if (state.hasSidedTransparency()) stateFlags = stateFlags or HAS_SIDED_TRANSPARENCY
                if (state.isReplaceable) stateFlags = stateFlags or IS_REPLACEABLE
                if (state.isLiquid) stateFlags = stateFlags or IS_LIQUID
                if (state.isSolidBlock(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) stateFlags = stateFlags or IS_SOLID
                if (state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) stateFlags = stateFlags or IS_FULL_CUBE
                if (state.hasRandomTicks()) stateFlags = stateFlags or HAS_RANDOM_TICKS
                if (state.hasComparatorOutput()) stateFlags = stateFlags or HAS_COMPARATOR_OUTPUT

                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.DOWN)) sideFlags = sideFlags or DOWN_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP)) sideFlags = sideFlags or UP_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.NORTH)) sideFlags = sideFlags or NORTH_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.SOUTH)) sideFlags = sideFlags or SOUTH_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.WEST)) sideFlags = sideFlags or WEST_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.EAST)) sideFlags = sideFlags or EAST_SIDE_SOLID
                if (state.isSideSolid(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.DOWN, SideShapeType.CENTER)) sideFlags = sideFlags or DOWN_CENTER_SOLID
                if (state.isSideSolid(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP, SideShapeType.CENTER)) sideFlags = sideFlags or UP_CENTER_SOLID
                
                stateJson.addProperty("id", Block.getRawIdFromState(state))
                stateJson.addProperty("state_flags", stateFlags and 0xFFF)
                stateJson.addProperty("side_flags", sideFlags and 0xFF)
                stateJson.addProperty("instrument", state.instrument.name)
                stateJson.addProperty("luminance", state.luminance)
                stateJson.addProperty("piston_behavior", state.pistonBehavior.name)
                stateJson.addProperty("hardness", state.getHardness(null, null))
                if (state.isOpaque) 
                    stateJson.addProperty("opacity", state.opacity)

                if (block.defaultState == state) 
                    blockJson.addProperty("default_state_id", Block.getRawIdFromState(state))

                block.getStateWithProperties(state).let { stateWithProperties ->
                    val propsJson = JsonObject()
                    for (prop in block.stateManager.properties) {
                        val value = stateWithProperties.get(prop)
                        propsJson.addProperty(prop.name, value.toString())
                    }
                    stateJson.add("properties", propsJson)
                }

                val collisionShapeIdxsJson = JsonArray()
                for (box in state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).boundingBoxes) {
                    val idx = shapes.putIfAbsent(box, shapes.size)
                    collisionShapeIdxsJson.add(Objects.requireNonNullElseGet(idx) { shapes.size - 1 })
                }

                stateJson.add("collision_shapes", collisionShapeIdxsJson)

                val outlineShapeIdxsJson = JsonArray()
                for (box in state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).boundingBoxes) {
                    val idx = shapes.putIfAbsent(box, shapes.size)
                    outlineShapeIdxsJson.add(Objects.requireNonNullElseGet(idx) { shapes.size - 1 })
                }

                stateJson.add("outline_shapes", outlineShapeIdxsJson)

                for (blockEntity in Registries.BLOCK_ENTITY_TYPE) {
                    if (blockEntity.supports(state)) {
                        stateJson.addProperty("block_entity_type", Registries.BLOCK_ENTITY_TYPE.getRawId(blockEntity))
                    }
                }

                statesJson.add(stateJson)
            }
            blockJson.add("states", statesJson)

            blocksJson.add(blockJson)
        }

        val shapesJson = JsonArray()
        var gson = Gson()
        for (shape in shapes.keys) {
            val shapeJson = JsonObject()
            shapeJson.add("min", gson.toJsonTree(
                listOf(shape.minX, shape.minY, shape.minZ)
            ))
            shapeJson.add("max", gson.toJsonTree(
                listOf(shape.maxX, shape.maxY, shape.maxZ)
            ))
            shapesJson.add(shapeJson)
        }

        val blockEntitiesJson = JsonArray()
        for (blockEntity in Registries.BLOCK_ENTITY_TYPE) 
            blockEntitiesJson.add(Registries.BLOCK_ENTITY_TYPE.getId(blockEntity)!!.toString())

        return arrayOf(
            Pair(blocksJson, "blocks.json"),
            Pair(shapesJson, "block_shapes.json"),
            Pair(blockEntitiesJson, "block_entity_types.json"),
        )
    }
}
