package id.haho.muslimdaily.domain.rawatib

import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Position
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.Rawatib

/**
 * One line of the qabliyah → fard → ba'diyah timeline. Several entries on one line are
 * alternatives shown together, e.g. Jumu'ah ba'diyah "2 at home / 4 in the masjid".
 */
data class RawatibLine(val entries: List<Rawatib>) {
    val primary: Rawatib get() = entries.first()
}

data class PrayerPlan(
    /** The prayer as shown: DHUHR becomes JUMUAH on Friday. */
    val prayer: Prayer,
    val before: List<RawatibLine>,
    val fard: Fard?,
    val after: List<RawatibLine>,
    /** Friday: those who do not attend Jumu'ah pray Dhuhr. */
    val showJumuahNote: Boolean,
)

/**
 * Builds the timeline for one prayer from rawatib.json. All structure comes from data; the rules
 * here are the spec's display rules:
 *  - Friday Dhuhr shows as Jumu'ah, with no qabliyah row.
 *  - Travel mode hides every entry marked hidden_when_travelling (only Fajr sunnah stays).
 *  - A variant group with one default is a choice (e.g. 4 or 2 before Dhuhr); with several
 *    defaults the forms are shown together.
 *  - The general prayer between adhan and iqamah (prayer = null) appears only where the prayer has
 *    no specific qabliyah, so it is never listed twice.
 */
object RawatibPlanner {

    const val DHUHR_QABLIYAH_GROUP = "dhuhr_qabliyah"

    fun plan(
        prayer: Prayer,
        isFriday: Boolean,
        travelMode: Boolean,
        dhuhrQabliyahRakah: Int,
        rawatib: List<Rawatib>,
        fard: List<Fard>,
    ): PrayerPlan {
        val shown = if (prayer == Prayer.DHUHR && isFriday) Prayer.JUMUAH else prayer
        val visible = rawatib.filter { !travelMode || !it.hiddenWhenTravelling }
        val own = visible.filter { it.prayer == shown }

        var before = lines(own.filter { it.position == Position.BEFORE }, dhuhrQabliyahRakah)
        if (shown == Prayer.JUMUAH) before = emptyList()
        if (before.isEmpty() && shown != Prayer.JUMUAH) {
            before = lines(visible.filter { it.prayer == null && it.position == Position.BEFORE }, dhuhrQabliyahRakah)
        }
        val after = lines(own.filter { it.position == Position.AFTER }, dhuhrQabliyahRakah)

        return PrayerPlan(
            prayer = shown,
            before = before,
            fard = fard.firstOrNull { it.prayer == shown },
            after = after,
            showJumuahNote = shown == Prayer.JUMUAH,
        )
    }

    private fun lines(entries: List<Rawatib>, dhuhrQabliyahRakah: Int): List<RawatibLine> {
        val result = mutableListOf<RawatibLine>()
        val seenGroups = mutableSetOf<String>()
        for (entry in entries) {
            val group = entry.variantGroup
            if (group == null) {
                result += RawatibLine(listOf(entry))
                continue
            }
            if (!seenGroups.add(group)) continue
            val members = entries.filter { it.variantGroup == group }
            val defaults = members.filter { it.defaultVariant }
            result += if (defaults.size > 1) {
                RawatibLine(defaults)
            } else {
                val chosen = if (group == DHUHR_QABLIYAH_GROUP) {
                    members.firstOrNull { it.rakah == dhuhrQabliyahRakah }
                } else {
                    null
                }
                RawatibLine(listOf(chosen ?: defaults.firstOrNull() ?: members.first()))
            }
        }
        return result
    }
}
