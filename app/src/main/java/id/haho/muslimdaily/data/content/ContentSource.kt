package id.haho.muslimdaily.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Where content files come from. Today only the bundled assets exist. A future signed content pack
 * would be a second implementation that verifies the pack signature before returning any bytes
 * (see docs/architecture.md, "Content packs"); the importer itself would not change.
 */
interface ContentSource {
    /** Raw bytes of duas.json, rawatib.json or taxonomy.json. */
    fun read(name: String): ByteArray
}

class AssetContentSource @Inject constructor(@ApplicationContext private val context: Context) : ContentSource {
    override fun read(name: String): ByteArray = context.assets.open("content/$name").use { it.readBytes() }
}
