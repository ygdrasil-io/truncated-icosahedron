import com.soywiz.kmem.*
import com.soywiz.korag.AG
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korge.Korge
import com.soywiz.korge3d.*
import com.soywiz.korge3d.Shaders3D.Companion.a_col
import com.soywiz.korge3d.Shaders3D.Companion.a_norm
import com.soywiz.korge3d.Shaders3D.Companion.a_pos
import com.soywiz.korim.color.Colors
import geometry.icosahedron
import geometry.truncatedIcosahedron
import kotlin.random.Random

@Korge3DExperimental
suspend fun main() = Korge(width = 800, height = 800) {

    scene3D {
        light().position(0f, 2f, 0f).setTo(Colors.WHITE)
        val stage3D = this

        mesh(truncatedIcosahedronMesh())

        stage3D.camera.position.setTo(0f, 2f, 0f)
        stage3D.camera.lookAt(0f, 0f, 0f)

    }
}

@Korge3DExperimental
fun truncatedIcosahedronMesh(): Mesh3D {
    //uncomment to replace truncatedIcosahedron by a simpler icosahedron
    //val truncatedIcosahedron = icosahedron(1f, 0)
    val truncatedIcosahedron = truncatedIcosahedron(1f, 0)

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