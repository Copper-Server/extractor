package copper_server.extractor.extractors

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import copper_server.extractor.Extractor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import net.minecraft.network.NetworkSide
import net.minecraft.network.packet.PacketType
import net.minecraft.network.state.NetworkState
import net.minecraft.network.packet.StatusPackets
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class PacketField(val name: String, val type: String)

data class PacketInfo(val id: String, val className: String, val fields: List<PacketField>)

data class PacketDirectionData(val packets: Map<String, PacketInfo>)

data class ProtocolStateData(
        val clientbound: PacketDirectionData,
        val serverbound: PacketDirectionData
)

class Protocol : Extractor {
    private val modID: String = "copper_server extractor protocol"
    private val logger: Logger = LoggerFactory.getLogger(modID)

    private fun getDeclaredFieldRecursive(clazz: Class<*>, fieldName: String): Field {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                // Field not in this class, try the superclass
            }
            currentClass = currentClass.superclass
        }
        throw NoSuchFieldException("Field '$fieldName' not found in class hierarchy for '$clazz'")
    }

    override fun extract(server: MinecraftServer): JsonElement {
       // Intermediate map to group packets by Phase and Side before final structuring.
        val tempProtocolsData = mutableMapOf<String, MutableMap<NetworkSide, MutableMap<String, PacketInfo>>>()

        // A map of protocol phases to the classes that statically declare their PacketTypes.
        val packetRegistryClasses = mapOf(
            "handshake" to "net.minecraft.network.packet.HandshakePackets",
            "play" to "net.minecraft.network.packet.PlayPackets",
            "status" to "net.minecraft.network.packet.StatusPackets",
            "login" to "net.minecraft.network.packet.LoginPackets",
            "configuration" to "net.minecraft.network.packet.ConfigPackets"
        )

        val packetTypeClass = Class.forName("net.minecraft.network.packet.PacketType")

        for ((phaseId, className) in packetRegistryClasses) {
            try {
                val registryClass = Class.forName(className)
                // Find all public static fields of type PacketType<?>
                val packetTypeFields = registryClass.fields.filter {
                    Modifier.isStatic(it.modifiers) &&
                    packetTypeClass.isAssignableFrom(it.type)
                }

                for (field in packetTypeFields) {
                    val packetType = field.get(null) as? net.minecraft.network.packet.PacketType<*>
                    if (packetType == null) {
                        logger.warn("Could not get PacketType from field ${field.name} in $className")
                        continue
                    }

                    val side = packetType.side()
                    val id = packetType.id().toString() // Use the string Identifier as the ID

                    // Reflect on the field's generic type to get the actual packet class (e.g., T from PacketType<T>)
                    val fieldType = field.genericType
                    if (fieldType !is ParameterizedType || fieldType.actualTypeArguments.isEmpty()) {
                        logger.warn("Field ${field.name} in $className is not a generic PacketType")
                        continue
                    }

                    val typeArgument = fieldType.actualTypeArguments[0]
                    if (typeArgument !is Class<*>) {
                        logger.warn("PacketType generic argument for ${field.name} is not a class: $typeArgument")
                        continue
                    }

                    val packetClass = typeArgument
                    val fields = packetClass.declaredFields
                        .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                        .map { PacketField(it.name, it.genericType.typeName) }

                    val packetInfo = PacketInfo(id, packetClass.name, fields)
                    
                    val phasePackets = tempProtocolsData.getOrPut(phaseId) { mutableMapOf() }
                    val sidePackets = phasePackets.getOrPut(side) { mutableMapOf() }
                    sidePackets[packetClass.simpleName] = packetInfo
                }
            } catch (e: ClassNotFoundException) {
                logger.warn("Could not find packet registry class: $className. Skipping this phase.")
            } catch (e: Exception) {
                logger.error("Failed to process packet registry class: $className", e)
            }
        }
        
        // Transform the temporary map into the final structure for JSON serialization
        val allProtocolsData = tempProtocolsData.mapValues { (_, phaseData) ->
            ProtocolStateData(
                clientbound = PacketDirectionData((phaseData[NetworkSide.CLIENTBOUND] ?: emptyMap()).toSortedMap()),
                serverbound = PacketDirectionData((phaseData[NetworkSide.SERVERBOUND] ?: emptyMap()).toSortedMap())
            )
        }.toSortedMap()

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJsonTree(allProtocolsData)
    }
}
