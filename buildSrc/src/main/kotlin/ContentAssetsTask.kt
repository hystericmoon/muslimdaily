import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Copies the content in `/content` into the APK's assets.
 *
 * Every entry ships, so the build fails if one has placeholder Arabic, no source, or names an
 * audio file that does not exist. The full authoring validator lives in tools/build_content.py;
 * this is the last gate before shipping.
 */
@CacheableTask
abstract class ContentAssetsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val src = contentDir.get().asFile
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val contentOut = File(out, "content").apply { mkdirs() }
        val audioOut = File(out, "audio").apply { mkdirs() }

        @Suppress("UNCHECKED_CAST")
        val duas = (JsonSlurper().parse(File(src, "duas.json")) as Map<String, Any?>)["duas"] as List<Map<String, Any?>>
        val errors = duas.flatMap { validate(it, src) }
        if (errors.isNotEmpty()) {
            throw GradleException("Content is incomplete:\n" + errors.joinToString("\n") { " - $it" })
        }

        for (name in listOf("duas.json", "rawatib.json", "taxonomy.json")) {
            File(src, name).copyTo(File(contentOut, name), overwrite = true)
        }

        val audioSrc = File(src, "audio")
        val audioFiles = duas.mapNotNull { audioFileOf(it) }.toSet() + "silence.opus"
        audioFiles.forEach { name ->
            val file = File(audioSrc, name)
            if (file.exists()) file.copyTo(File(audioOut, name), overwrite = true)
        }
    }

    private fun audioFileOf(entry: Map<String, Any?>): String? =
        ((entry["audio"] as? Map<*, *>)?.get("file") as? String)?.takeIf { it.isNotBlank() }

    private fun validate(entry: Map<String, Any?>, src: File): List<String> {
        val id = entry["id"]
        val problems = mutableListOf<String>()
        val arabic = entry["arabic"] as? String
        if (arabic.isNullOrBlank() || arabic.contains("[PLACEHOLDER")) problems += "$id: arabic text missing"
        val sources = entry["sources"] as? List<*>
        val hasSource = sources?.any { (it as? Map<*, *>)?.get("book")?.toString()?.isNotBlank() == true } == true
        if (!hasSource) problems += "$id: source missing"
        val grading = entry["grading"] as? Map<*, *>
        if (!grading?.get("grade")?.toString().isNullOrBlank() && grading?.get("grader")?.toString().isNullOrBlank()) {
            problems += "$id: grade without grader"
        }
        val file = audioFileOf(entry)
        if (file != null && !File(src, "audio/$file").exists()) problems += "$id: audio file $file missing"
        return problems
    }
}
