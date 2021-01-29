package geometry
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * port from https://github.com/thallada/icosahedron/blob/master/src/lib.rs
 */

const val VERT_CACHE_PRECISION = 10000f

data class Triangle(
    val a: Int,
    val b: Int,
    val c: Int
)

class Polyhedron(
    val positions: MutableList<Vector3> = mutableListOf(),
    val cells: MutableList<Triangle> = mutableListOf(),
    val normals: MutableList<Vector3> = mutableListOf(),
    private val added_vert_cache: MutableMap<Triple<Int, Int, Int>, Int> = mutableMapOf(),
    val faces: MutableList<List<Int>> = mutableListOf()
) {

    fun addPosition(vertex: Vector3): Int {
        val vertexKey = Triple(
            (vertex.x * VERT_CACHE_PRECISION).roundToInt(),
            (vertex.y * VERT_CACHE_PRECISION).roundToInt(),
            (vertex.z * VERT_CACHE_PRECISION).roundToInt()
        )
        return added_vert_cache.getOrElse(vertexKey) {
            positions.add(vertex)
            normals.add(Vector3(0f, 0f, 0f))
            val addedIndex = positions.size - 1
            added_vert_cache[vertexKey] = addedIndex
            return addedIndex
        }
    }

    fun trianglesToFaces() {
        cells.forEachIndexed { index, _ ->
            faces.add(listOf(index))
        }
    }

    fun subdivide(other: Polyhedron, radius: Float, detail: Int) {
        other.cells.forEach { triangle ->
            val a = other.positions[triangle.a]
            val b = other.positions[triangle.b]
            val c = other.positions[triangle.c]
            subdivideTriangle(a, b, c, radius, detail)
        }
    }

    private fun subdivideTriangle(a: Vector3, b: Vector3, c: Vector3, radius: Float, detail: Int) {
        val cols = 2f.pow(detail).toInt()

        val newVertices = mutableListOf<MutableList<Vector3>>()

        (0..cols).forEach { i ->

            newVertices.add(mutableListOf())
            val aj = a.lerp(c, i.toFloat() / cols.toFloat())
            val bj = b.lerp(c, i.toFloat() / cols.toFloat())
            val rows = cols - i
            (0..rows).forEach { j ->
                if (j == 0 && i == cols) {
                    newVertices[i].add(aj.normalize() * radius)
                } else {
                    newVertices[i]
                        .add(aj.lerp(bj, j.toFloat() / rows.toFloat()).normalize() * radius)
                }
            }
        }

        (0 until cols).forEach { i ->
            (0 until 2 * (cols - i) - 1).forEach { j ->
                val k = j / 2

                val triangle = if (j % 2 == 0) {
                    Triangle(
                        addPosition(newVertices[i][k + 1]),
                        addPosition(newVertices[i + 1][k]),
                        addPosition(newVertices[i][k])
                    )
                } else {
                    Triangle(
                        addPosition(newVertices[i][k + 1]),
                        addPosition(newVertices[i + 1][k + 1]),
                        addPosition(newVertices[i + 1][k])
                    )
                }

                cells.add(triangle)
            }
        }
    }

    fun computeTriangleNormals() {
        val origin = Vector3(0f, 0f, 0f)
        val cellWindingToFix = mutableMapOf<Int, Triangle>()
        cells.forEachIndexed { index, cell ->

            val vertexA = positions[cell.a]
            val vertexB = positions[cell.b]
            val vertexC = positions[cell.c]

            val e1 = vertexA - vertexB
            val e2 = vertexC - vertexB
            var no = e1.cross(e2)

            // detect and correct inverted normal or winding order
            val dist = vertexB - origin
            if (no.dot(dist) < 0.0) {
                no *= -1f
            } else {
                cellWindingToFix[index] = Triangle(cell.c, cell.b, cell.a)
            }

            normals[cell.a] += no
            normals[cell.b] += no
            normals[cell.c] += no

        }

        cellWindingToFix.forEach { (index, triangle) -> cells[index] = triangle }

        normals.forEachIndexed { index, normal ->
            normals[index] = normal.normalize()
        }
    }

    fun truncated(other: Polyhedron) {

        val vertToFaces = other.vertToFaces()
        val originalVertCount = other.positions.size
        val triangleCentroids = other.triangleCentroids()
        val midCentroidCache = mutableMapOf<Triple<Int, Int, Int>, Vector3>()
        var hexCount = 0
        var pentCount = 0
        (0 until originalVertCount).forEach { i ->
            val faces = vertToFaces[i]
                ?: throw error("index $i not found on vertex to faces")
            if (faces.size == 6) {
                hexCount += 1
            } else {
                pentCount += 1
            }

            val centerPoint = findCenterOfTriangles(faces, triangleCentroids)

            val newFace = mutableListOf<Int>()

            faces.forEach { face_index ->
                val triangle = other.cells[face_index]
                val otherVerts = listOf(triangle.a, triangle.b, triangle.c)
                    .filter { vertex -> vertex != i }

                val sortedTriangle = Triangle(i, otherVerts[0], otherVerts[1])

                val centroid = triangleCentroids[face_index]
                    ?: throw error("fail to find centroid")
                val midBCentroid = other.calculateMidCentroid(
                    sortedTriangle.a,
                    sortedTriangle.b,
                    faces,
                    face_index,
                    centroid,
                    triangleCentroids,
                    midCentroidCache
                )
                val midCCentroid = other.calculateMidCentroid(
                    sortedTriangle.a,
                    sortedTriangle.c,
                    faces,
                    face_index,
                    centroid,
                    triangleCentroids,
                    midCentroidCache
                )

                val centerPointIndex = addPosition(centerPoint)
                val centroidIndex = addPosition(centroid)
                val midBCentroidIndex = addPosition(midBCentroid)
                val midCCentroidIndex = addPosition(midCCentroid)

                cells.add(
                    Triangle(
                        centerPointIndex,
                        midCCentroidIndex,
                        centroidIndex
                    )
                )
                newFace.add(cells.size - 1)
                cells.add(
                    Triangle(
                        centerPointIndex,
                        centroidIndex,
                        midBCentroidIndex
                    )
                )
                newFace.add(cells.size - 1)
            }
            this.faces.add(newFace)
        }
    }

    private fun calculateMidCentroid(
        spoke_vertex_index: Int,
        vertex_index: Int,
        faces: List<Int>,
        currentFaceIndex: Int,
        centroid: Vector3,
        triangleCentroids: Map<Int, Vector3>,
        midCentroidCache: MutableMap<Triple<Int, Int, Int>, Vector3>
    ): Vector3 {
        val adjFaceIndex = findAdjacentFace(spoke_vertex_index, vertex_index, faces, currentFaceIndex)
            ?: throw error("fail to find adjacent face")
        val adjCentroid = triangleCentroids[adjFaceIndex]
            ?: throw error("fail to find adjacent centroid")

        val key = Triple(spoke_vertex_index, vertex_index, adjFaceIndex)
        return when (val midCentroid1 = midCentroidCache[key]) {
            null -> {
                val midCentroid = centroid.lerp(adjCentroid, 0.5f)
                midCentroidCache[key] = midCentroid
                midCentroid
            }
            else -> midCentroid1
        }
    }

    private fun findAdjacentFace(
        spokeVertexIndex: Int,
        vertexIndex: Int,
        faces: List<Int>,
        currentFaceIndex: Int
    ): Int? {
        return faces.asSequence()
            .filter { face_index -> face_index != currentFaceIndex }
            .find { face_index ->
                val triangle = cells[face_index]
                ((triangle.a == spokeVertexIndex
                        || triangle.b == spokeVertexIndex
                        || triangle.c == spokeVertexIndex)
                        && (triangle.a == vertexIndex
                        || triangle.b == vertexIndex
                        || triangle.c == vertexIndex))
            }
    }


    private fun findCenterOfTriangles(triangle_indices: List<Int>, triangle_centroids: Map<Int, Vector3>): Vector3 {
        var centerPoint = Vector3(0f, 0f, 0f)
        triangle_indices.forEach { triangle_index ->
            centerPoint += triangle_centroids[triangle_index]
                ?: throw error("index $triangle_index not found on triangle centroids")
        }
        centerPoint /= triangle_indices.size.toFloat()
        return centerPoint
    }


    private fun triangleCentroids(): Map<Int, Vector3> {
        return mutableMapOf<Int, Vector3>().apply {
            cells.forEachIndexed { i, cell ->
                val a = positions[cell.a]
                val b = positions[cell.b]
                val c = positions[cell.c]
                this[i] = calculateCentroid(a, b, c)
            }
        }
    }

    private fun calculateCentroid(pa: Vector3, pb: Vector3, pc: Vector3): Vector3 {
        val vab_half = (pb - pa) / 2f
        val pab_half = pa + vab_half
        return ((pc - pab_half) * (1f / 3f)) + pab_half
    }

    private fun vertToFaces(): Map<Int, MutableList<Int>> {
        return mutableMapOf<Int, MutableList<Int>>()
            .apply {
                cells.forEachIndexed { index, triangle ->

                    when (val faces = this[triangle.a]) {
                        null -> this[triangle.a] = mutableListOf(index)
                        else -> faces.add(index)
                    }

                    when (val faces = this[triangle.b]) {
                        null -> this[triangle.b] = mutableListOf(index)
                        else -> faces.add(index)
                    }

                    when (val faces = this[triangle.c]) {
                        null -> this[triangle.c] = mutableListOf(index)
                        else -> faces.add(index)
                    }
                }
            }
    }

}


fun truncatedIcosahedron(radius: Float, detail: Int): Polyhedron {
    return Polyhedron().apply {
        val icosahedron = icosahedron(radius, detail)
        truncated(icosahedron)
        computeTriangleNormals()
    }
}


fun icosahedron(radius: Float, detail: Int): Polyhedron {
    val baseIcosahedron = Polyhedron(
        cells = mutableListOf(
            Triangle(0, 11, 5),
            Triangle(0, 5, 1),
            Triangle(0, 1, 7),
            Triangle(0, 7, 10),
            Triangle(0, 10, 11),
            Triangle(1, 5, 9),
            Triangle(5, 11, 4),
            Triangle(11, 10, 2),
            Triangle(10, 7, 6),
            Triangle(7, 1, 8),
            Triangle(3, 9, 4),
            Triangle(3, 4, 2),
            Triangle(3, 2, 6),
            Triangle(3, 6, 8),
            Triangle(3, 8, 9),
            Triangle(4, 9, 5),
            Triangle(2, 4, 11),
            Triangle(6, 2, 10),
            Triangle(8, 6, 7),
            Triangle(9, 8, 1)
        )
    ).apply {
        val t = (1f + sqrt(5f)) / 2f
        addPosition(Vector3(-1f, t, 0f))
        addPosition(Vector3(1f, t, 0f))
        addPosition(Vector3(-1f, -t, 0f))
        addPosition(Vector3(1f, -t, 0f))
        addPosition(Vector3(0f, -1f, t))
        addPosition(Vector3(0f, 1f, t))
        addPosition(Vector3(0f, -1f, -t))
        addPosition(Vector3(0f, 1f, -t))
        addPosition(Vector3(t, 0f, -1f))
        addPosition(Vector3(t, 0f, 1f))
        addPosition(Vector3(-t, 0f, -1f))
        addPosition(Vector3(-t, 0f, 1f))
    }

    return Polyhedron().apply {
        subdivide(baseIcosahedron, radius, detail)
        trianglesToFaces()
    }
}

