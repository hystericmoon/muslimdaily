package id.haho.muslimdaily.data.handover

import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class HandoverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `packages pair by suffix`() {
        assertEquals("id.haho.adwam.debug", Handover.newPackageFor("id.haho.muslimdaily.debug"))
        assertEquals("id.haho.adwam", Handover.newPackageFor("id.haho.muslimdaily"))
        assertNull(Handover.newPackageFor("id.haho.adwam.debug"))
    }

    @Test
    fun `nothing has moved until the new app says so`() {
        assertNull(Handover.movedTo(context))
        Handover.markMoved(context, "id.haho.adwam.debug")
        assertEquals("id.haho.adwam.debug", Handover.movedTo(context))
    }

    @Test
    fun `an app without the signature permission gets nothing`() {
        val authority = Handover.authority(context.packageName)
        val provider = Robolectric.buildContentProvider(HandoverProvider::class.java)
            .create(ProviderInfo().apply { this.authority = authority })
            .get()
        assertFailsWith<SecurityException> { provider.openFile(Uri.parse("content://$authority/${Handover.PATH_BACKUP}"), "r") }
        assertFailsWith<SecurityException> { provider.call(Handover.METHOD_HANDED_OVER, null, null) }
        assertNull(Handover.movedTo(context))
    }
}
