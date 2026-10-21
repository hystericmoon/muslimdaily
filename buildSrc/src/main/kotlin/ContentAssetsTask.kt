import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Copies the reviewed content in `/content` into the APK's assets.
 *
 * Release builds (includeDrafts = false) only receive entries whose status is "published",
 * and the build fails if a published entry is missing its source, grading or audio.
 * The full authoring validator lives in tools/build_content.py; this is the last gate before shipping.
 */
@CacheableTask
abstract class ContentAssetsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDir: DirectoryProperty

    @get:Input
    abstract val includeDrafts: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val src = contentDir.get().asFile
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val contentOut = File(out, "content").apply { mkdirs() }
        val audioOut = File(out, "audio").apply { mkdirs() }
        val drafts = includeDrafts.get()

        @Suppress("UNCHECKED_CAST")
        val duasRoot = JsonSlurper().parse(File(src, "duas.json")) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val duas = duasRoot["duas"] as List<Map<String, Any?>>
        val shipped = duas.filter { drafts || it["status"] == "published" }

        val errors = mutableListOf<String>()
        shipped.filter { it["status"] == "published" }.forEach { errors += validatePublished(it, src) }
        if (errors.isNotEmpty()) {
            throw GradleException("Published content is incomplete:\n" + errors.joinToString("\n") { " - $it" })
        }

        duasRoot["duas"] = shipped
        File(contentOut, "duas.json").writeText(JsonOutput.toJson(duasRoot))

        @Suppress("UNCHECKED_CAST")
        val rawatibRoot = JsonSlurper().parse(File(src, "rawatib.json")) as MutableMap<String, Any?>
        for (key in listOf("fard", "rawatib")) {
            @Suppress("UNCHECKED_CAST")
            val rows = rawatibRoot[key] as List<Map<String, Any?>>
            rawatibRoot[key] = rows.filter { drafts || it["status"] == "published" }
        }
        File(contentOut, "rawatib.json").writeText(JsonOutput.toJson(rawatibRoot))

        File(src, "taxonomy.json").copyTo(File(contentOut, "taxonomy.json"), overwrite = true)

        val audioSrc = File(src, "audio")
        val audioFiles = shipped.mapNotNull { audioFileOf(it) }.toSet() + "silence.opus"
        audioFiles.forEach { name ->
            val file = File(audioSrc, name)
            if (file.exists()) file.copyTo(File(audioOut, name), overwrite = true)
        }
    }

    private fun audioFileOf(entry: Map<String, Any?>): String? =
        (entry["audio"] as? Map<*, *>)?.get("file") as? String

    private fun validatePublished(entry: Map<String, Any?>, src: File): List<String> {
        val id = entry["id"]
        val problems = mutableListOf<String>()
        val arabic = entry["arabic"] as? String
        if (arabic.isNullOrBlank() || arabic.contains("[PLACEHOLDER")) problems += "$id: arabic text missing"
        val sources = entry["sources"] as? List<*>
        val hasSource = sources?.any { (it as? Map<*, *>)?.get("book")?.toString()?.isNotBlank() == true } == true
        if (!hasSource) problems += "$id: source missing"
        val grading = entry["grading"] as? Map<*, *>
        if (grading?.get("grade")?.toString().isNullOrBlank() || grading?.get("grader")?.toString().isNullOrBlank()) {
            problems += "$id: grading or grader missing"
        }
        val audio = entry["audio"] as? Map<*, *>
        val file = audio?.get("file") as? String
        val segments = audio?.get("segments") as? List<*>
        if (file.isNullOrBlank() || !File(src, "audio/$file").exists()) problems += "$id: audio file missing"
        if (segments.isNullOrEmpty()) problems += "$id: audio segments missing"
        return problems
    }
}
