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

    /** طلبات تجريبية — كتبان فلوحة تحكم التاجر فوض تجريبي. */
    val orders: List<Order> = listOf(
        Order(
            id = "o-demo-1",
            ts = System.currentTimeMillis() - 42 * 60_000L,
            buyer = "يوسف",
            fulfillment = "الاستلام من الحانوت",
            items = listOf(OrderItem("حليب 1 لتر", 2, 8.0), OrderItem("خبز", 3, 1.5)),
            total = 20.5
        ),
        Order(
            id = "o-demo-2",
            ts = System.currentTimeMillis() - 3 * 3_600_000L,
            buyer = "سعاد",
            fulfillment = "طلب التوصيل (حسب موافقة التاجر)",
            note = "عافاك عيّط ليا قبل ما تجي",
            items = listOf(OrderItem("زيت 1 لتر", 1, 19.0), OrderItem("سكر 1 كيلو", 1, 7.0)),
            total = 26.0
        ),
        Order(
            id = "o-demo-3",
            ts = System.currentTimeMillis() - 26 * 3_600_000L,
            buyer = "كريم",
            fulfillment = "الاستلام من الحانوت",
            items = listOf(OrderItem("قهوة", 1, 15.0), OrderItem("ماء 1.5 لتر", 4, 6.0)),
            total = 39.0,
            status = Order.STATUS_DONE
        )
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
