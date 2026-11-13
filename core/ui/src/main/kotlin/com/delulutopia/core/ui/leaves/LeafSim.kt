package com.delulutopia.core.ui.leaves

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Falling leaves (doc "Delululand — Falling Leaves"; web src/motion/leaves/sim.ts): the simulation,
 * free of the UI so it can be tested with a seeded random source. Coordinates are px of the layer;
 * surfaces arrive fresh every frame; a resting leaf keeps an offset from its surface, so it scrolls
 * with it. Numbers are the web's, scaled by [unit] (px per CSS px).
 */
enum class SurfaceKind { Flat, Ground, Round }

class Surface(
    /** Identity for this frame; a resting leaf follows it. */
    val key: Any,
    /** Stable id across remounts, so a leaf re-attaches after an excluded screen. */
    val id: String?,
    val kind: SurfaceKind,
    var l: Float,
    var r: Float,
    val top: Float,
    val b: Float,
    val cx: Float = 0f,
    val cy: Float = 0f,
    val rad: Float = 0f,
) {
    /** Ground only: spans held by the round button. */
    var walls: List<Pair<Float, Float>> = emptyList()
}

/** The layer and the band between the pinned bars that cards are visible in. */
data class LeafView(val w: Float, val h: Float, val clipTop: Float, val clipBottom: Float)

private data class Budget(val air: Int, val item: Int, val ground: Int)

/** §6, level Rich, phone. */
private val BUDGET = Budget(air = 30, item = 14, ground = 16)
private const val CONTACT_SHARE = 0.45
private const val FRONT_SCALE = 0.78f
private const val TAU = (PI * 2).toFloat()

/** Half-length and half-width of each species, as a share of the leaf's size. */
internal val SPECIES = listOf(0.46f to 0.45f, 0.46f to 0.3f, 0.46f to 0.25f, 0.44f to 0.36f)
internal const val COLOURS = 5

enum class LeafState { Fall, Rest, Slide }

class Leaf(val id: Int, val contact: Boolean, var x: Float, var y: Float, val size: Float, val sp: Int, val col: Int) {
    var vx = 0f
    var vy = 0f
    var resp = 1f
    var vTerm = 60f
    var swayPh = 0f
    var swaySpd = 2f
    var swayAmp = 20f
    var flip = 0f
    var flipSpd = 1f
    var tumble = false
    var rotV = 2f
    var tumbleRot = 0f
    var spin = 0f
    var baseRot = 0f
    var rot = 0f
    var state = LeafState.Fall
    var st = 0f
    var surf: Any? = null
    var surfId: String? = null
    var ground: Boolean? = null
    var clipped = false
    var offX = 0f
    var sv = 0f
    var slip = false
    var tipped = false
    var tipDir = 0f
    var life = 0f
    var alpha = 0f
    var fading = false
    var restRot = 0f
    var restFlip = 0f
    var bounce = 0f
    var noHit = 0f
    var dead = false
}

private fun sgn(v: Float) = if (v < 0) -1f else 1f

private fun normAngle(a: Float): Float {
    var n = (a + PI.toFloat()) % TAU
    if (n < 0) n += TAU
    return n - PI.toFloat()
}

private class Segment(val l: Float, val r: Float, val wl: Boolean, val wr: Boolean)

class LeafSim(private val unit: Float, private val random: Random = Random.Default) {
    val leaves = mutableListOf<Leaf>()
    var t = 0f
        private set
    private var windNow = 0f
    private var gustNow = 0f
    private var gustStrength = 0f
    private var gustStart = -99f
    private var gustDur = 1f
    private var gustPeak = 0f
    private var gustNext = 5f
    private var gustDir = 1f
    private var nextSpawn = 0.1f
    private var idSeq = 0
    private var seedRest = false
    private var view = LeafView(0f, 0f, 0f, 0f)
    private var surfaces: List<Surface> = emptyList()
    private var byKey: Map<Any, Surface> = emptyMap()
    private var byId: Map<String, Surface> = emptyMap()
    private val viewGround = Any()
    val started get() = leaves.isNotEmpty() || t > 0

    private fun rand(a: Float, b: Float) = a + random.nextFloat() * (b - a)

    private fun randi(n: Int) = floor(random.nextFloat() * n).toInt().coerceAtMost(n - 1)

    /** §6 pre-warm: the screen opens already in motion, and half the resting budgets lie on cards and the bar. */
    fun start(v: LeafView) {
        view = v
        leaves.clear()
        t = 0f
        nextSpawn = 0.1f
        gustStart = -99f
        gustNext = rand(4f, 7f)
        repeat((BUDGET.air * 0.6).roundToInt()) {
            val l = makeLeaf(rand(0f, v.w), rand(-0.1f, 0.75f) * v.h)
            l.vy = l.vTerm * 0.8f
            leaves += l
        }
        seedRest = true
    }

    private fun makeLeaf(x: Float, y: Float, contact: Boolean = random.nextFloat() < CONTACT_SHARE): Leaf {
        val a = 20f * unit
        val b = 30f * unit
        val size = rand(a, b) * if (contact) 1f else FRONT_SCALE
        val small = ((1 - (size - a) / (b - a))).coerceIn(0f, 1f)
        return Leaf(idSeq++, contact, x, y, size, randi(SPECIES.size), randi(COLOURS)).apply {
            vy = rand(20f, 40f) * unit
            resp = 0.75f + 0.45f * small
            vTerm = rand(48f, 76f) * unit * (size / (26f * unit)).pow(0.3f)
            swayPh = rand(0f, TAU)
            swaySpd = rand(1.5f, 2.6f)
            swayAmp = rand(14f, 30f) * unit
            flip = rand(0f, TAU)
            flipSpd = rand(0.45f, 1.3f)
            tumble = random.nextFloat() < 0.22f
            rotV = rand(1.4f, 2.8f) * if (random.nextFloat() < 0.5f) -1 else 1
            baseRot = rand(-0.4f, 0.4f)
        }
    }

    /** Half the vertical extent of a leaf as drawn now: its contact point (§4). */
    fun extentY(l: Leaf): Float {
        val fx = max(0.15f, abs(cos(l.flip)))
        val (hl, hw) = SPECIES[l.sp]
        return l.size * hl * abs(cos(l.rot)) + l.size * hw * fx * abs(sin(l.rot))
    }

    private fun counts(): Triple<Int, Int, Int> {
        var air = 0
        var item = 0
        var ground = 0
        for (l in leaves) {
            if (l.fading) continue
            when {
                l.state == LeafState.Fall -> air++
                l.ground == true -> ground++
                else -> item++
            }
        }
        return Triple(air, item, ground)
    }

    fun step(dt: Float, v: LeafView, list: List<Surface>) {
        view = v
        setSurfaces(list)
        if (seedRest) seedResting()
        t += dt
        wind()
        if (t >= nextSpawn) {
            if (counts().first < BUDGET.air) spawn()
            nextSpawn = t + rand(0.12f, 0.28f) * if (t < 4) 0.5f else 1f
        }
        for (l in leaves) {
            l.st += dt
            if (l.fading) {
                l.alpha -= dt / 0.6f
            } else if (l.alpha < 1) {
                l.alpha = min(1f, l.alpha + dt / 0.4f)
            }
            if (l.state == LeafState.Fall) stepFall(l, dt) else stepAttached(l, dt)
        }
        leaves.removeAll { (it.fading && it.alpha <= 0) || it.dead }
        // §6 over budget: the oldest card leaf slips off, the oldest bar leaf fades.
        for (ground in listOf(false, true)) {
            val on = leaves.filter { !it.fading && it.state == LeafState.Rest && it.ground == ground && it.life > 0 }.sortedBy { it.life }
            val cap = if (ground) BUDGET.ground else BUDGET.item
            for (i in 0 until on.size - cap) if (ground) on[i].fading = true else on[i].life = 0f
        }
    }

    private fun setSurfaces(list0: List<Surface>) {
        val list = if (list0.any { it.kind == SurfaceKind.Ground }) {
            list0
        } else {
            list0 +
                Surface(viewGround, "view-ground", SurfaceKind.Ground, 0f, view.w, view.h, view.h)
        }
        val rounds = list.filter { it.kind == SurfaceKind.Round }
        for (s in list) {
            if (s.kind != SurfaceKind.Ground) continue
            s.l = 0f
            s.r = view.w
            s.walls = rounds.filter { it.cy - it.rad <= s.top && it.cy + it.rad >= s.top }.map { (it.cx - it.rad - 2 * unit) to (it.cx + it.rad + 2 * unit) }
        }
        surfaces = list
        byKey = list.associateBy { it.key }
        byId = list.mapNotNull { s -> s.id?.let { it to s } }.toMap()
    }

    private fun segOf(s: Surface, x: Float): Segment? {
        if (s.kind != SurfaceKind.Ground) return Segment(s.l, s.r, wl = false, wr = false)
        var l = s.l
        var r = s.r
        for ((a, b) in s.walls) {
            if (x in a..b) return null
            if (b <= x && b > l) l = b
            if (a >= x && a < r) r = a
        }
        return Segment(l, r, wl = true, wr = true)
    }

    private fun landable(s: Surface) = s.kind != SurfaceKind.Flat || (s.top >= view.clipTop + 4 * unit && s.top <= view.clipBottom - 6 * unit)

    private fun seedResting() {
        seedRest = false
        val flats = surfaces.filter { it.kind == SurfaceKind.Flat && landable(it) && it.r - it.l > 60 * unit }
        val ground = surfaces.firstOrNull { it.kind == SurfaceKind.Ground }
        fun put(s: Surface) {
            val l = makeLeaf(0f, 0f, contact = true)
            l.flip = 0f
            l.rot = (if (random.nextFloat() < 0.5f) -1 else 1) * 1.2f
            val x = rand(s.l + l.size * 0.6f, s.r - l.size * 0.6f)
            val seg = segOf(s, x) ?: return
            l.x = x
            l.y = s.top - extentY(l)
            land(l, s, seg)
            l.state = LeafState.Rest
            l.sv = 0f
            l.bounce = 0f
            l.rot = l.restRot
            l.flip = l.restFlip
            l.life *= rand(0.3f, 1f)
            leaves += l
        }
        if (flats.isNotEmpty()) repeat((BUDGET.item * 0.5).roundToInt()) { put(flats[randi(flats.size)]) }
        if (ground != null) repeat((BUDGET.ground * 0.5).roundToInt()) { put(ground) }
    }

    private fun wind() {
        val base = 12f * unit * (sin(t / 17) * 0.8f + sin(t / 7.3f + 1) * 0.45f)
        if (t >= gustNext) startGust()
        val u = (t - gustStart) / gustDur
        gustNow = if (u in 0f..1f) gustPeak * sin(PI.toFloat() * u).pow(2) else 0f
        gustStrength = abs(gustNow) / (150f * unit)
        windNow = base + gustNow
    }

    private fun startGust() {
        gustDir = if (random.nextFloat() < 0.7f) gustDir else -gustDir
        gustStart = t
        gustDur = rand(1.4f, 2.6f)
        gustPeak = rand(90f, 150f) * unit * gustDir
        gustNext = t + gustDur + rand(7f, 15f)
    }

    private fun spawn() {
        val l = makeLeaf(0f, 0f)
        if (abs(gustNow) > 60 * unit && random.nextFloat() < 0.5f) {
            l.x = if (gustNow > 0) -l.size else view.w + l.size
            l.y = rand(0.05f, 0.45f) * view.h
            l.vx = gustNow * 0.6f
        } else {
            l.x = rand(-0.1f, 1.1f) * view.w - windNow * 1.5f
            l.y = -l.size
        }
        leaves += l
    }

    /** §3 Land: rest or slide, and the resting pose. */
    private fun land(l: Leaf, s: Surface, seg: Segment) {
        l.surf = s.key
        l.surfId = s.id
        l.ground = s.kind == SurfaceKind.Ground
        l.clipped = s.kind == SurfaceKind.Flat
        l.offX = l.x - s.l
        l.st = 0f
        l.rot = normAngle(l.rot)
        l.tumble = false
        l.tumbleRot = 0f
        l.spin = 0f
        l.tipped = false
        val w = l.size * 0.8f
        val dl = l.x - seg.l
        val dr = seg.r - l.x
        var edge = false
        var sv = 0f
        val mode = when {
            s.kind == SurfaceKind.Flat && min(dl, dr) < 0.4f * w -> {
                edge = true
                sv = (if (dl < dr) -1 else 1) * rand(25f, 45f) * unit
                LeafState.Slide
            }
            abs(l.vx) > 70 * unit -> {
                sv = l.vx * 0.6f
                LeafState.Slide
            }
            random.nextFloat() < (if (s.kind == SurfaceKind.Ground) 0.85f else 0.5f) -> LeafState.Rest
            else -> {
                sv = (if (random.nextFloat() < 0.5f) -1 else 1) * rand(30f, 70f) * unit
                LeafState.Slide
            }
        }
        l.state = mode
        l.sv = sv
        l.slip = edge
        l.restRot = (if (l.rot >= 0) 1 else -1) * (PI.toFloat() / 2 - rand(0.1f, 0.45f))
        l.restFlip = (l.flip / PI.toFloat()).roundToInt() * PI.toFloat()
        l.life = if (s.kind == SurfaceKind.Ground) rand(30f, 60f) else rand(6f, 14f)
        l.bounce = min(4f * unit, l.vy * 0.05f)
    }

    private fun release(l: Leaf, vx: Float, vy: Float) {
        l.state = LeafState.Fall
        l.surf = null
        l.surfId = null
        l.ground = null
        l.clipped = false
        l.st = 0f
        l.slip = false
        l.tipped = false
        l.vx = vx
        l.vy = vy
        l.tumbleRot = 0f
        l.baseRot = l.rot - 0.5f * sin(l.swayPh)
        l.noHit = 0.15f
    }

    private fun stepFall(l: Leaf, dt: Float) {
        l.noHit = max(0f, l.noHit - dt)
        l.swayPh += l.swaySpd * dt
        val swayK = if (l.tipped) (l.st / 3).coerceIn(0.15f, 1f) else 1f
        val swayV = l.swayAmp * swayK * l.swaySpd * cos(l.swayPh)
        val tvx = windNow * l.resp + swayV + if (l.tipped && l.st < 5) l.tipDir * 10 * unit else 0f
        l.vx += (tvx - l.vx) * min(1f, dt / 0.45f)
        val ff = abs(cos(l.flip))
        val lift = -0.22f * abs(gustNow)
        val tvy = l.vTerm * (0.5f + 0.55f * abs(cos(l.swayPh)) + 0.3f * (1 - ff)) + lift * l.resp
        l.vy += (tvy - l.vy) * min(1f, dt / 0.4f)
        if (l.tumble) l.tumbleRot += l.rotV * dt
        l.baseRot += l.spin * dt
        l.spin *= exp(-dt * 2.2f)
        val target = l.baseRot + 0.5f * sin(l.swayPh) + l.tumbleRot
        l.rot += (target - l.rot) * min(1f, dt / 0.25f)
        l.flip += l.flipSpd * TAU * dt * if (l.tumble) 1.6f else 1f

        val prevBottom = l.y + extentY(l)
        l.x += l.vx * dt
        l.y += l.vy * dt
        val bottom = l.y + extentY(l)

        if (l.vy > 0 && l.noHit <= 0) {
            for (s in surfaces) {
                if (s.kind == SurfaceKind.Round) {
                    val dx = l.x - s.cx
                    val dy = bottom - s.cy
                    if (dy < 0 && dx * dx + dy * dy < s.rad * s.rad) {
                        val dir = if (abs(dx) < 1) (if (random.nextFloat() < 0.5f) -1f else 1f) else sgn(dx)
                        l.y -= bottom - (s.cy - sqrt(max(0f, s.rad * s.rad - dx * dx)))
                        l.vx = dir * rand(55f, 85f) * unit
                        l.vy = -rand(5f, 20f) * unit
                        l.spin = dir * rand(2f, 4f)
                        l.noHit = 0.2f
                        break
                    }
                    continue
                }
                if (!(prevBottom <= s.top + 0.5f * unit && bottom >= s.top)) continue
                if (s.kind == SurfaceKind.Flat && (!l.contact || l.x < s.l || l.x > s.r || !landable(s))) continue
                val seg = segOf(s, l.x) ?: continue
                if (s.kind == SurfaceKind.Ground) l.x = l.x.coerceIn(seg.l + l.size * 0.55f, max(seg.l + l.size * 0.55f, seg.r - l.size * 0.55f))
                l.y = s.top - extentY(l)
                land(l, s, seg)
                break
            }
        }
        if (l.x < -80 * unit || l.x > view.w + 80 * unit || l.y > view.h + 60 * unit || l.y < -220 * unit) l.dead = true
    }

    private fun stepAttached(l: Leaf, dt: Float) {
        var s = l.surf?.let { byKey[it] }
        if (s == null && l.surfId != null) {
            s = byId[l.surfId!!]
            if (s != null) l.surf = s.key
        }
        if (s == null) return release(l, rand(-15f, 15f) * unit, 10f * unit)
        val seg = segOf(s, s.l + l.offX) ?: Segment(s.l, s.r, wl = false, wr = false)
        // A gust nudges a resting leaf into a slide or lifts it off (§4).
        if (!l.fading && random.nextFloat() < gustStrength * dt * if (l.ground == true) 0.25f else 0.6f) {
            if (random.nextFloat() < 0.55f) {
                l.state = LeafState.Slide
                l.sv = sgn(windNow) * rand(40f, 90f) * unit * max(0.4f, gustStrength)
            } else {
                return release(l, windNow * 0.5f, -rand(40f, 90f) * unit * max(0.5f, gustStrength))
            }
        }
        if (l.state == LeafState.Slide) {
            l.offX += l.sv * dt
            val fr = (
                if (l.slip) {
                    0f
                } else if (l.ground == true) {
                    110f
                } else {
                    80f
                }
                ) * unit
            val nv = l.sv - sgn(l.sv) * fr * dt
            l.sv = if (sgn(nv) == sgn(l.sv)) nv else 0f
            if (abs(l.sv) < 3 * unit) {
                l.state = LeafState.Rest
                l.sv = 0f
                l.st = 0f
            }
        }
        l.x = s.l + l.offX
        val over = l.size * 0.12f
        if (l.x < seg.l - over || l.x > seg.r + over) {
            val dir = if (l.x < seg.l) -1f else 1f
            if ((dir < 0 && seg.wl) || (dir > 0 && seg.wr)) {
                l.x = if (dir < 0) seg.l + l.size * 0.55f else seg.r - l.size * 0.55f
                l.offX = l.x - s.l
                l.state = LeafState.Rest
                l.sv = 0f
            } else {
                // §3 Tip: over the edge and falling again.
                l.rot = normAngle(l.rot)
                release(l, dir * rand(18f, 32f) * unit, 15f * unit)
                l.spin = dir * rand(2.5f, 4f)
                l.tipped = true
                l.tipDir = dir
                return
            }
        }
        val breathe = if (l.state == LeafState.Rest) 0.04f * sin(t * 2.3f + l.id) * (0.3f + gustStrength) else sgn(l.sv) * 0.15f
        l.rot += (l.restRot + breathe - l.rot) * min(1f, dt / 0.22f)
        l.flip += (l.restFlip - l.flip) * min(1f, dt / 0.22f)
        val bounce = l.bounce * abs(sin(l.st * 12)) * exp(-l.st * 6)
        l.y = s.top - extentY(l) + l.size * 0.05f - bounce
        if (l.state == LeafState.Rest && !l.fading) {
            l.life -= dt
            if (l.life <= 0) {
                // §3 Expire: on a card most slip toward the nearer edge and fall on; the rest fade.
                if (l.ground != true && random.nextFloat() < 0.65f) {
                    l.state = LeafState.Slide
                    l.slip = true
                    l.sv = (if (seg.r - l.x < l.x - seg.l) 1 else -1) * rand(50f, 80f) * unit
                } else {
                    l.fading = true
                }
            }
        }
    }
}
