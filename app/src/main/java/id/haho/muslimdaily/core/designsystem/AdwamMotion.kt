package id.haho.muslimdaily.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Adwam's motion numbers, in one place (skill `motion`, `docs/roadmap-2026-11.md`). Screens use
 * these and never repeat a number. Everything animates a graphics-layer transform or alpha, never
 * layout size, and snaps when [reduced] is true (the system's remove-animations setting, which
 * `HahoTheme.motion.reduced` reports).
 */
object AdwamMotion {
    /** Touch feedback: press, toggle, row highlight. */
    const val INSTANT = 90

    /** Things that appear: menus, the island pill, hints. */
    const val QUICK = 160

    /** Cards expanding, sheets, tab changes, a highlight moving between rows. */
    const val STANDARD = 260

    /** Rare one-off moments: a ring filling, first launch. */
    const val SLOW = 420

    /** Something arriving on screen: fast start, long settle. */
    val Arrive = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Something leaving: slow start, quick end. Keep it shorter than the arrival. */
    val Leave = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** Something moving across the screen from one place to another. */
    val Across = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** The mark-as-read pulse: one soft scale up and back. */
    const val MARK_PULSE_SCALE = 1.08f

    /** A shared highlight moving between rows (Today's current prayer). */
    fun <T> slide(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(STANDARD, easing = Across)

    /** A stroke or mark drawing itself in (the check). */
    fun <T> draw(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(STANDARD, easing = Arrive)

    /** The up half of a pulse. */
    fun <T> pulseUp(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(INSTANT, easing = Arrive)

    /** The settle half of a pulse. */
    fun <T> pulseDown(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(QUICK, easing = Arrive)

    /** Physical things only (sheets, the island, the Qibla needle): damped, no bounce for reward. */
    fun <T> physical(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
}
