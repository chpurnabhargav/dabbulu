package app.dabbulu.ui.art

/** The 3D illustration for each category, plus the "More" tile. Coordinates are on a 64x64 grid. */
object CategoryArt {

    fun of(categoryId: String): Art = byId[categoryId] ?: Other

    private fun lin(vararg s: Pair<Float, Long>, x1: Float = 0f, y1: Float = 0f, x2: Float = 0f, y2: Float = 1f) =
        Linear(s.toList(), x1, y1, x2, y2)

    private fun rad(vararg s: Pair<Float, Long>, cx: Float = 0.5f, cy: Float = 0.5f, r: Float = 0.5f) =
        Radial(s.toList(), cx, cy, r)

    private fun solid(c: Long) = Solid(c)

    private val white = solid(0xFFFFFFFF)

    private fun shadow(cx: Float = 32f, cy: Float = 57f, rx: Float = 22f, ry: Float = 4f) =
        Layer(Ellipse(cx, cy, rx, ry), rad(0f to 0x99000000, 1f to 0x00000000L))

    private fun highlight(d: String, width: Float = 2.2f, alpha: Float = 0.6f, rotate: Float = 0f) =
        Layer(PathGeo(d), stroke = white, strokeWidth = width, alpha = alpha, rotate = rotate)

    private fun seed(x: Float, y: Float, r: Float) =
        Layer(Ellipse(x, y, 1.8f, 1f), solid(0xFFFFF6DD), rotate = r, pivotX = x, pivotY = y)

    val Food = Art(listOf(
        shadow(32f, 57f, 22f, 4.5f),
        Layer(RRect(11f, 42f, 42f, 11f, 5.5f), lin(0f to 0xFFFBBF6A, 1f to 0xFFB45F1C)),
        Layer(RRect(9f, 35f, 46f, 9f, 4.5f), lin(0f to 0xFF8B4A26, 1f to 0xFF3F1D0B)),
        Layer(PathGeo("M10 33h44v2.5l-7 5-5.5-4-7 5-6.5-5-6.5 5-6.5-4.5-5 3.5z"), solid(0xFFFFC83D)),
        Layer(PathGeo("M7 33Q10 29 13 32T19 32T25 32T31 32T37 32T43 32T49 32T55 32Q57 33 57 35H7Z"), lin(0f to 0xFFA3F07F, 1f to 0xFF2F9E44)),
        Layer(
            PathGeo("M9 30c0-11 10.3-19 23-19s23 8 23 19c0 1.7-1.3 3-3 3H12c-1.7 0-3-1.3-3-3z"),
            rad(0f to 0xFFFFE0A3, 0.5f to 0xFFF59E42, 1f to 0xFFB45309, cx = 0.38f, cy = 0.28f, r = 0.75f),
        ),
        Layer(Ellipse(24f, 18.5f, 7.5f, 3f), white, alpha = 0.5f, rotate = -18f, pivotX = 24f, pivotY = 18.5f),
        seed(31f, 21f, -20f), seed(39f, 18f, 25f), seed(42f, 25f, -10f), seed(22f, 26f, 15f), seed(32f, 27f, 0f),
    ))

    val Snacks = Art(listOf(
        shadow(32f, 58f, 24f, 4f),
        Layer(Ellipse(31f, 51f, 24f, 6f), lin(0f to 0xFFF1F4FA, 1f to 0xFF9AA3B8)),
        Layer(Ellipse(31f, 50f, 17f, 3.6f), solid(0xFFC9D0DE)),
        Layer(PathGeo("M46 29h2.5a7 7 0 0 1 0 14H45"), stroke = solid(0xFFB7BFD0), strokeWidth = 4f),
        Layer(
            PathGeo("M13 26h36v11c0 8.3-8 14-18 14s-18-5.7-18-14z"),
            lin(0f to 0xFFFFFFFF, 0.55f to 0xFFE4E8F1, 1f to 0xFF9AA3B8, x2 = 1f, y2 = 0f),
        ),
        Layer(RRect(13f, 31f, 36f, 5f), solid(0xFFF59E0B), alpha = 0.85f),
        Layer(Ellipse(31f, 26f, 18f, 4.5f), solid(0xFFF8FAFF)),
        Layer(Ellipse(31f, 26.4f, 15.5f, 3.4f), rad(0f to 0xFFD8894A, 1f to 0xFF7A3A12, cx = 0.4f, cy = 0.4f, r = 0.7f)),
        highlight("M17.5 30c0 7 1.5 12 5 15", width = 2.5f, alpha = 0.8f),
        Layer(PathGeo("M25 19c-2.5-3 2.5-5 0-9"), stroke = solid(0xFFE5E7EB), strokeWidth = 2.2f, alpha = 0.7f),
        Layer(PathGeo("M34 19c-2.5-3 2.5-5 0-9"), stroke = solid(0xFFE5E7EB), strokeWidth = 2.2f, alpha = 0.5f),
    ))

    val Transport = Art(listOf(
        shadow(32f, 57f, 26f, 4f),
        Layer(PathGeo("M18 33l4.8-9.6C23.8 21.3 25.9 20 28.2 20h9.6c2.3 0 4.4 1.3 5.4 3.4L48 33z"), lin(0f to 0xFF93C5FD, 1f to 0xFF2563EB)),
        Layer(PathGeo("M23.5 31.5l3.3-6.8c.5-1 1.5-1.7 2.7-1.7h2v8.5z"), lin(0f to 0xFFF0F9FF, 1f to 0xFF7DB7F0, x2 = 1f, y2 = 1f)),
        Layer(PathGeo("M34.5 23h2c1.2 0 2.2.7 2.7 1.7l3.3 6.8h-8z"), lin(0f to 0xFFF0F9FF, 1f to 0xFF7DB7F0, x2 = 1f, y2 = 1f)),
        Layer(
            PathGeo("M7 41c0-4.4 3.6-8 8-8h34c4.4 0 8 3.6 8 8v5c0 2.2-1.8 4-4 4H11c-2.2 0-4-1.8-4-4z"),
            lin(0f to 0xFF7CC0FF, 0.5f to 0xFF2F7BEF, 1f to 0xFF1E3A8A),
        ),
        highlight("M13 37h38", width = 2f, alpha = 0.45f),
        Layer(RRect(51f, 39f, 5f, 3.5f, 1.75f), solid(0xFFFFE58A)),
        Layer(RRect(8f, 39f, 4f, 3.5f, 1.75f), solid(0xFFFCA5A5)),
        Layer(Circle(20f, 50f, 6.5f), solid(0xFF111827)),
        Layer(Circle(20f, 50f, 3f), rad(0f to 0xFFF9FAFB, 1f to 0xFF8B93A3, cx = 0.4f, cy = 0.35f, r = 0.65f)),
        Layer(Circle(44f, 50f, 6.5f), solid(0xFF111827)),
        Layer(Circle(44f, 50f, 3f), rad(0f to 0xFFF9FAFB, 1f to 0xFF8B93A3, cx = 0.4f, cy = 0.35f, r = 0.65f)),
    ))

    val Groceries = Art(listOf(
        shadow(32f, 58f, 24f, 4f),
        Layer(Circle(46f, 21f, 7f), rad(0f to 0xFFBBF7D0, 0.5f to 0xFF22C55E, 1f to 0xFF14532D, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(38f, 17f, 7.5f), rad(0f to 0xFFBBF7D0, 0.5f to 0xFF22C55E, 1f to 0xFF14532D, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(41f, 25f, 7f), rad(0f to 0xFFBBF7D0, 0.5f to 0xFF22C55E, 1f to 0xFF14532D, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(23f, 23f, 9f), rad(0f to 0xFFFFB4B4, 0.5f to 0xFFEF4444, 1f to 0xFF991B1B, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(PathGeo("M23 14.5c0-3 1-5 3-6"), stroke = solid(0xFF5B3A1A), strokeWidth = 2f),
        Layer(PathGeo("M25.5 11.5c3-3 7-2 8 0-3 2-6 2-8 0z"), solid(0xFF22C55E)),
        Layer(Ellipse(19.5f, 19.5f, 3f, 2f), white, alpha = 0.55f, rotate = -30f, pivotX = 19.5f, pivotY = 19.5f),
        Layer(
            PathGeo("M9 31h46l-5.2 19.5c-.6 2.4-2.8 4-5.3 4H19.5c-2.5 0-4.7-1.6-5.3-4z"),
            lin(0f to 0xFFFCD34D, 0.6f to 0xFFD97706, 1f to 0xFF92400E),
        ),
        Layer(PathGeo("M21 34l2 18M32 34v18M43 34l-2 18M11.5 42h41"), stroke = solid(0xFF7C2D12), strokeWidth = 1.6f, alpha = 0.35f),
        Layer(RRect(6f, 27f, 52f, 7.5f, 3.75f), lin(0f to 0xFFFDE68A, 1f to 0xFFB45309)),
        highlight("M11 29.5h28", width = 1.8f, alpha = 0.55f),
    ))

    val Shopping = Art(listOf(
        shadow(32f, 58f, 22f, 4f),
        Layer(PathGeo("M23 26v-7a9 9 0 0 1 18 0v7"), stroke = solid(0xFF831843), strokeWidth = 3.5f),
        Layer(
            PathGeo("M14 23h36l3.2 27.5c.3 2.6-1.7 4.8-4.3 4.8H15.1c-2.6 0-4.6-2.2-4.3-4.8z"),
            lin(0f to 0xFFFBCFE8, 0.45f to 0xFFEC4899, 1f to 0xFF9D174D, x2 = 1f, y2 = 1f),
        ),
        Layer(PathGeo("M43 23h7l3.2 27.5c.3 2.6-1.7 4.8-4.3 4.8H45z"), solid(0xFF000000), alpha = 0.14f),
        Layer(Circle(23f, 28f, 1.8f), solid(0xFF831843)),
        Layer(Circle(41f, 28f, 1.8f), solid(0xFF831843)),
        highlight("M17 28l-1.8 20", width = 2.5f, alpha = 0.6f),
    ))

    val Bills = Art(listOf(
        shadow(32f, 58f, 18f, 3.6f),
        Layer(PathGeo("M38.5 8.5L16.5 38.5h14l-4 21 24-32H36.5z"), solid(0xFF92400E)),
        Layer(PathGeo("M36 6L14 36h14l-4 21 24-32H34z"), lin(0f to 0xFFFFF7B8, 0.45f to 0xFFFACC15, 1f to 0xFFCA8A04, x2 = 1f, y2 = 1f)),
        highlight("M33.5 11L19.5 31", width = 2.2f, alpha = 0.7f),
    ))

    val Family = Art(listOf(
        shadow(32f, 58f, 20f, 4f),
        Layer(
            PathGeo("M33.5 55.5S9.5 41.5 9.5 25.5c0-7 5.5-12 12-12 5 0 9 3 12 7 3-4 7-7 12-7 6.5 0 12 5 12 12 0 16-24 30-24 30z"),
            solid(0xFF881337),
        ),
        Layer(
            PathGeo("M32 54S8 40 8 24c0-7 5.5-12 12-12 5 0 9 3 12 7 3-4 7-7 12-7 6.5 0 12 5 12 12 0 16-24 30-24 30z"),
            rad(0f to 0xFFFFC2CF, 0.45f to 0xFFF43F5E, 1f to 0xFF9F1239, cx = 0.35f, cy = 0.3f, r = 0.75f),
        ),
        Layer(Ellipse(19f, 21f, 5.5f, 3.2f), white, alpha = 0.6f, rotate = -35f, pivotX = 19f, pivotY = 21f),
        Layer(Circle(44f, 20f, 1.6f), white, alpha = 0.7f),
    ))

    val Rent = Art(listOf(
        shadow(32f, 58f, 24f, 4f),
        Layer(RRect(41f, 13f, 6f, 12f, 1f), solid(0xFFB91C1C)),
        Layer(RRect(14f, 28f, 36f, 27f, 3f), lin(0f to 0xFFFFF4E0, 1f to 0xFFD9A877)),
        Layer(PathGeo("M40 28h7a3 3 0 0 1 3 3v21a3 3 0 0 1-3 3h-7z"), solid(0xFF000000), alpha = 0.12f),
        Layer(
            PathGeo("M7.5 30.5L30 10.2c1.1-1 2.9-1 4 0L56.5 30.5c1 .9.3 2.5-1 2.5H8.5c-1.3 0-2-1.6-1-2.5z"),
            lin(0f to 0xFFFCA5A5, 0.5f to 0xFFEF4444, 1f to 0xFF991B1B),
        ),
        highlight("M14 28L30 14", width = 2f, alpha = 0.45f),
        Layer(RRect(27f, 39f, 10f, 16f, 2f), lin(0f to 0xFFB45309, 1f to 0xFF713F12)),
        Layer(Circle(34.5f, 47.5f, 1f), solid(0xFFFDE68A)),
        Layer(RRect(17.5f, 35f, 7f, 7f, 1.5f), lin(0f to 0xFFE0F2FE, 1f to 0xFF38BDF8, x2 = 1f, y2 = 1f)),
        Layer(RRect(39.5f, 35f, 7f, 7f, 1.5f), lin(0f to 0xFFE0F2FE, 1f to 0xFF38BDF8, x2 = 1f, y2 = 1f)),
    ))

    val Subscriptions = Art(listOf(
        shadow(32f, 58f, 22f, 3.6f),
        Layer(PathGeo("M25 53h14l-2.5-7h-9z"), lin(0f to 0xFF6B7280, 1f to 0xFF374151)),
        Layer(RRect(7f, 11f, 50f, 36f, 7f), lin(0f to 0xFF4B5563, 1f to 0xFF111827)),
        Layer(RRect(11f, 15f, 42f, 28f, 4f), lin(0f to 0xFFFCA5A5, 0.5f to 0xFFEF4444, 1f to 0xFF7F1D1D, x2 = 1f, y2 = 1f)),
        Layer(PathGeo("M13 17h16L15 37z"), white, alpha = 0.14f),
        Layer(
            PathGeo("M28 22.5v13c0 .8.9 1.3 1.6.9l10.2-6.5c.6-.4.6-1.4 0-1.8L29.6 21.6c-.7-.4-1.6.1-1.6.9z"),
            white, alpha = 0.95f,
        ),
    ))

    private val puff = rad(0f to 0xFFFFFFFF, 0.55f to 0xFFFEF3C7, 1f to 0xFFFCD34D, cx = 0.38f, cy = 0.32f, r = 0.7f)

    val Entertainment = Art(listOf(
        shadow(32f, 58f, 20f, 3.6f),
        Layer(Circle(22f, 19f, 7f), puff),
        Layer(Circle(42f, 19f, 7f), puff),
        Layer(Circle(32f, 14f, 8f), puff),
        Layer(Circle(27f, 23f, 6f), puff),
        Layer(Circle(37f, 23f, 6f), puff),
        Layer(PathGeo("M14 22h36l-4 30c-.3 2.3-2.3 4-4.6 4H22.6c-2.3 0-4.3-1.7-4.6-4z"), lin(0f to 0xFFFFFFFF, 1f to 0xFFE5E7EB, x2 = 1f, y2 = 0f)),
        Layer(PathGeo("M19 22h6l2 34h-5zM29 22h6l-.5 34h-5zM39 22h6l-3 34h-5z"), solid(0xFFEF4444), alpha = 0.92f),
        Layer(PathGeo("M43 22h7l-4 30c-.3 2.3-2.3 4-4.6 4H40z"), solid(0xFF000000), alpha = 0.12f),
        Layer(RRect(12f, 19.5f, 40f, 5f, 2.5f), lin(0f to 0xFFFFFFFF, 1f to 0xFFD1D5DB)),
        highlight("M17.5 27L20 50", width = 2f, alpha = 0.5f),
    ))

    val Health = Art(listOf(
        shadow(32f, 57f, 20f, 3.6f),
        Layer(PathGeo("M19 23H32V41H19A9 9 0 0 1 19 23Z"), rad(0f to 0xFFFCA5A5, 0.5f to 0xFFEF4444, 1f to 0xFF991B1B, cx = 0.4f, cy = 0.3f, r = 0.8f), rotate = -38f),
        Layer(PathGeo("M32 23H45A9 9 0 0 1 45 41H32Z"), lin(0f to 0xFFFFFFFF, 1f to 0xFFC8CDD6), rotate = -38f),
        highlight("M21 27H43", width = 2.5f, alpha = 0.55f, rotate = -38f),
        Layer(Circle(48f, 15f, 7.5f), rad(0f to 0xFF99F6E4, 0.5f to 0xFF14B8A6, 1f to 0xFF115E59, cx = 0.35f, cy = 0.3f, r = 0.75f)),
        Layer(PathGeo("M48 11v8M44 15h8"), stroke = white, strokeWidth = 2.4f),
    ))

    val Personal = Art(listOf(
        shadow(32f, 58f, 18f, 3.6f),
        Layer(RRect(37f, 13f, 10f, 3.2f, 1.6f), solid(0xFF9CA3AF)),
        Layer(RRect(24f, 11f, 15f, 7f, 2f), lin(0f to 0xFFF3F4F6, 1f to 0xFF9CA3AF)),
        Layer(RRect(27f, 17f, 10f, 8f, 1.5f), lin(0f to 0xFFE5E7EB, 1f to 0xFF9CA3AF, x2 = 1f, y2 = 0f)),
        Layer(RRect(18f, 24f, 28f, 32f, 7f), lin(0f to 0xFFFBCFE8, 0.5f to 0xFFF472B6, 1f to 0xFF9D174D, x2 = 1f, y2 = 0f)),
        Layer(RRect(22f, 34f, 20f, 12f, 3f), white, alpha = 0.85f),
        Layer(PathGeo("M32 36.5c2 2.6 3 4 3 5.4a3 3 0 0 1-6 0c0-1.4 1-2.8 3-5.4z"), solid(0xFFEC4899)),
        highlight("M22 29v20", width = 2.5f, alpha = 0.5f),
    ))

    val Other = Art(listOf(
        shadow(32f, 58f, 22f, 3.6f),
        Layer(RRect(12f, 31f, 40f, 23f, 3f), lin(0f to 0xFFC4B5FD, 0.5f to 0xFF8B5CF6, 1f to 0xFF5B21B6)),
        Layer(RRect(29f, 31f, 6f, 23f), lin(0f to 0xFFFDE68A, 1f to 0xFFF59E0B)),
        Layer(RRect(9f, 24f, 46f, 9f, 3f), lin(0f to 0xFFDDD6FE, 1f to 0xFF7C3AED)),
        Layer(RRect(29f, 24f, 6f, 9f), solid(0xFFFCD34D)),
        Layer(PathGeo("M32 24c-6-9-16-7-12-1 2 3 8 2 12 1zM32 24c6-9 16-7 12-1-2 3-8 2-12 1z"), rad(0f to 0xFFFDE68A, 0.5f to 0xFFF59E0B, 1f to 0xFFB45309, cx = 0.5f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(32f, 24f, 2.6f), solid(0xFFD97706)),
        highlight("M13 27h14", width = 1.8f, alpha = 0.55f),
    ))

    val More = Art(listOf(
        shadow(32f, 58f, 22f, 3.6f),
        Layer(Circle(21f, 20f, 9.5f), rad(0f to 0xFFE9D5FF, 0.5f to 0xFFA855F7, 1f to 0xFF581C87, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(43f, 20f, 9.5f), rad(0f to 0xFFA5F3FC, 0.5f to 0xFF06B6D4, 1f to 0xFF155E75, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(21f, 42f, 9.5f), rad(0f to 0xFFFDE68A, 0.5f to 0xFFF59E0B, 1f to 0xFF92400E, cx = 0.35f, cy = 0.3f, r = 0.7f)),
        Layer(Circle(43f, 42f, 9.5f), rad(0f to 0xFFBFDBFE, 0.5f to 0xFF3B82F6, 1f to 0xFF1E3A8A, cx = 0.35f, cy = 0.3f, r = 0.7f)),
    ))

    private val byId: Map<String, Art> = mapOf(
        "food" to Food,
        "snacks" to Snacks,
        "transport" to Transport,
        "groceries" to Groceries,
        "shopping" to Shopping,
        "bills" to Bills,
        "family" to Family,
        "rent" to Rent,
        "subscriptions" to Subscriptions,
        "entertainment" to Entertainment,
        "health" to Health,
        "personal" to Personal,
        "other" to Other,
    )

    val all: Map<String, Art> get() = byId
}
