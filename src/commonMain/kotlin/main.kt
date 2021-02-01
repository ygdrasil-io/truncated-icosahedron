import com.soywiz.kds.values
import com.soywiz.kmem.*
import com.soywiz.korag.AG
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korge.Korge
import com.soywiz.korge3d.*
import com.soywiz.korge3d.Shaders3D.Companion.a_col
import com.soywiz.korge3d.Shaders3D.Companion.a_norm
import com.soywiz.korge3d.Shaders3D.Companion.a_pos
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.applicationVfs
import com.soywiz.korio.serialization.xml.buildXml
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.unaryMinus
import geometry.truncatedIcosahedron
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

@Korge3DExperimental
suspend fun main() = Korge(width = 800, height = 800) {

    scene3D {
        light().position(0f, 0f, 5f).setTo(Colors.WHITE)
        val stage3D = this

        truncatedIcosahedronMesh()
        //val library = resourcesVfs["monkey-smooth.dae"].readColladaLibrary()
        val library = applicationVfs["truncated-icosahedron.dae"].readColladaLibrary()
        val model = library.geometryDefs.values.first()
        mesh(model.mesh).rotation(-90.degrees, 0.degrees, 0.degrees)


        stage3D.camera.position.setTo(0f, 0f, 5f)
        stage3D.camera.lookAt(0f, 0f, 0f)

    }
}

@Korge3DExperimental
fun truncatedIcosahedronMesh(): Mesh3D {
    //uncomment to replace truncatedIcosahedron by a simpler icosahedron
    //val truncatedIcosahedron = icosahedron(1f, 0)

    val id = "Chapin"
    val truncatedIcosahedron = truncatedIcosahedron(1f, 1)
    val xml = buildXml(
        "COLLADA",
        Pair("xmlns", "http://www.collada.org/2005/11/COLLADASchema"),
        Pair("version", "1.4.1"),
        Pair("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
    ) {

        node("library_geometries") {
            node("geometry", Pair("id", "$id-mesh"), Pair("name", id)) {
                node("mesh") {
                    node("source", Pair("id", "$id-mesh-positions")) {
                        val positions = truncatedIcosahedron.positions.flatMap { listOf(it.x, it.y, it.z) }
                        node("float_array", Pair("id", "$id-mesh-positions-array"), Pair("count", positions.size)) {
                            text(positions.joinToString(separator = " "))
                        }
                        node("technique_common") {
                            node(
                                "accessor",
                                Pair("source", "#$id-mesh-positions-array"),
                                Pair("stride", "3"),
                                Pair("count", truncatedIcosahedron.positions.size)
                            ) {
                                node("param", Pair("name", "X"), Pair("type", "float"))
                                node("param", Pair("name", "Y"), Pair("type", "float"))
                                node("param", Pair("name", "Z"), Pair("type", "float"))
                            }
                        }
                    }
                    node("source", Pair("id", "$id-mesh-normals")) {
                        val normals = truncatedIcosahedron.normals.flatMap { listOf(it.x, it.y, it.z) }
                        node("float_array", Pair("id", "$id-mesh-normals-array"), Pair("count", normals.size)) {
                            text(normals.joinToString(separator = " "))
                        }
                        node("technique_common") {
                            node(
                                "accessor",
                                Pair("source", "#$id-mesh-normals-array"),
                                Pair("stride", "3"),
                                Pair("count", truncatedIcosahedron.normals.size)
                            ) {
                                node("param", Pair("name", "X"), Pair("type", "float"))
                                node("param", Pair("name", "Y"), Pair("type", "float"))
                                node("param", Pair("name", "Z"), Pair("type", "float"))
                            }
                        }
                    }

                    node("source", Pair("id", "$id-mesh-colors")) {
                        val color = truncatedIcosahedron.cells.flatMap {
                            listOf(
                                Random.nextFloat(),
                                Random.nextFloat(),
                                Random.nextFloat()
                            )
                        }
                        node("float_array", Pair("id", "$id-mesh-colors-array"), Pair("count", color.size)) {
                            text(color.joinToString(separator = " "))
                        }
                        node("technique_common") {
                            node(
                                "accessor",
                                Pair("source", "#$id-mesh-colors-array"),
                                Pair("stride", "3"),
                                Pair("count", truncatedIcosahedron.cells.size)
                            ) {
                                node("param", Pair("name", "R"), Pair("type", "float"))
                                node("param", Pair("name", "G"), Pair("type", "float"))
                                node("param", Pair("name", "B"), Pair("type", "float"))
                            }
                        }
                    }

                    node("vertices", Pair("id", "$id-mesh-vertices")) {
                        node("input", Pair("semantic", "POSITION"), Pair("source", "#$id-mesh-positions"))
                        node("input", Pair("semantic", "NORMAL"), Pair("source", "#$id-mesh-normals"))
                    }
                    node("triangles", Pair("count", truncatedIcosahedron.cells.size)) {
                        node(
                            "input",
                            Pair("semantic", "VERTEX"),
                            Pair("source", "#$id-mesh-vertices"),
                            Pair("offset", "0")
                        )
                        //node("input", Pair("semantic", "COLOR"), Pair("source", "#$id-mesh-colors"), Pair("offset", "1"))
                        node("p") {
                            val triangles = truncatedIcosahedron.cells.flatMap { listOf(it.a, it.b, it.c) }
                            text(triangles.joinToString(separator = " "))
                        }

                    }
                }
            }
        }

        node("library_visual_scenes") {
            node("visual_scene", Pair("id", "scene"), Pair("name", "scene")) {
                node("node", Pair("type", "NODE"), Pair("id", id), Pair("name", id)) {
                    node("instance_geometry", Pair("url", "#$id-mesh"), Pair("name", id))
                }
            }
        }
        node("scene") {
            node("instance_visual_scene", Pair("url", "#Scene"))
        }
    }

    launchImmediately(Dispatchers.Main) {
        applicationVfs["truncated-icosahedron.dae"].writeString(xml.toString())
    }

    val vertexBufferUnitSize = (a_pos.type.bytesSize * a_pos.type.elementCount
            + a_norm.type.bytesSize * a_norm.type.elementCount
            + a_col.type.bytesSize * a_col.type.elementCount)
    val vertexBufferSize = truncatedIcosahedron.positions.size * vertexBufferUnitSize
    val vertexBuffer = MemBufferAlloc(vertexBufferSize)
    val vertexFloatBuffer = vertexBuffer.asFloat32Buffer()
    truncatedIcosahedron.positions
        .forEachIndexed { index, vector3 ->
            val normal = truncatedIcosahedron.normals[index]
            var vertexIndex = index * (a_pos.type.elementCount + a_norm.type.elementCount + a_col.type.elementCount)
            // Vertex
            vertexFloatBuffer[vertexIndex++] = vector3.x
            vertexFloatBuffer[vertexIndex++] = vector3.y
            vertexFloatBuffer[vertexIndex++] = vector3.z

            // Normal
            vertexFloatBuffer[vertexIndex++] = normal.x
            vertexFloatBuffer[vertexIndex++] = normal.y
            vertexFloatBuffer[vertexIndex++] = normal.z

            // Color
            vertexFloatBuffer[vertexIndex++] = Random.nextFloat()
            vertexFloatBuffer[vertexIndex++] = Random.nextFloat()
            vertexFloatBuffer[vertexIndex] = Random.nextFloat()
        }

    val indicesBuffer = MemBufferAlloc(truncatedIcosahedron.cells.size * 3 * Short.SIZE_BYTES)
    val indicesShortBuffer = vertexBuffer.asInt16Buffer()
    truncatedIcosahedron.cells
        .forEachIndexed { index, triangle ->
            var indiceIndex = index * 3
            indicesShortBuffer[indiceIndex++] = triangle.a.toShort()
            indicesShortBuffer[indiceIndex++] = triangle.b.toShort()
            indicesShortBuffer[indiceIndex] = triangle.c.toShort()
        }

    return Mesh3D(
        FBuffer(vertexBuffer),
        FBuffer(indicesBuffer),
        AG.IndexType.USHORT,
        truncatedIcosahedron.positions.size,
        VertexLayout(
            a_pos,
            a_norm,
            a_col
        ),
        null,
        AG.DrawType.TRIANGLES
    )
}