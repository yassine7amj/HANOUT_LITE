package ma.hanout.lite.data

/** معطيات تجريبية — كتّستعمل ملي التطبيق ما مربوطش مع Firebase. */
object DemoData {

    val products: List<Product> = listOf(
        Product("p1", "حليب 1 لتر", 8.0, 10.0, "حليب ومشتقاته", "🥛", true),
        Product("p2", "خبز", 1.5, null, "مخبوزات", "🥖", true),
        Product("p3", "سكر 1 كيلو", 7.0, 9.0, "مواد غذائية", "🍬", true),
        Product("p4", "زيت 1 لتر", 19.0, null, "مواد غذائية", "🫒", true),
        Product("p5", "ماء 1.5 لتر", 6.0, null, "مشروبات", "💧", true),
        Product("p6", "قهوة", 15.0, 18.0, "مشروبات", "☕", true)
    )

    val categories: List<Category> = listOf(
        Category("حليب ومشتقاته", "🥛"),
        Category("مخبوزات", "🥖"),
        Category("مواد غذائية", "🍬"),
        Category("مشروبات", "☕")
    )

    fun shop(slug: String): Shop = Shop(
        slug = slug,
        name = "حانوت الحومة — متجر تجريبي",
        phone = "",
        color = "",
        active = true,
        products = products,
        categories = categories
    )
}
