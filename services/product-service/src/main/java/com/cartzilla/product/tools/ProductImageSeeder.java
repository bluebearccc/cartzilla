package com.cartzilla.product.tools;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standalone one-off seeding script — KHÔNG phải Spring bean, không tự chạy khi app start.
 * Bổ sung thêm sản phẩm demo vào dữ liệu đã có (V2__seed_dev_data.sql) nhưng ảnh được upload
 * thật lên Cloudinary (thay vì trỏ thẳng tới picsum.photos như V2).
 *
 * Chạy thủ công 1 lần, từ thư mục services/product-service:
 *   mvn compile
 *   mvn spring-boot:run -Dspring-boot.run.main-class=com.cartzilla.product.tools.ProductImageSeeder
 *
 * (Yêu cầu Postgres của product-service và các biến CLOUDINARY_* / DB_* đã sẵn sàng,
 *  ví dụ chạy qua docker-compose như bình thường. Script tự bỏ qua sản phẩm đã tồn tại
 *  theo slug nên chạy lại nhiều lần vẫn an toàn, không upload trùng ảnh.)
 */
public final class ProductImageSeeder {

    private record Variant(String sku, String size, String color, String colorHex, long price, int stock) {}

    private record SeedProduct(String slug, String name, String description, long basePrice,
                                String tags, boolean featured, String categorySlug, String vendorSlug,
                                List<String> sourceImageUrls, List<Variant> variants) {}

    private static final List<SeedProduct> PRODUCTS = List.of(
            new SeedProduct(
                    "ao-hoodie-unisex-ni-bong",
                    "Áo hoodie unisex nỉ bông",
                    "Hoodie nỉ bông dày dặn, giữ ấm tốt, unisex form rộng.",
                    349_000, "ao,hoodie,unisex,ni", true,
                    "ao-thun", "local-supplier",
                    List.of(
                            "https://picsum.photos/seed/cartzilla-hoodie-1/800/1000",
                            "https://picsum.photos/seed/cartzilla-hoodie-2/800/1000"
                    ),
                    List.of(
                            new Variant("HOD-101-S-GRY", "S", "Xám", "#808080", 349_000, 20),
                            new Variant("HOD-101-M-GRY", "M", "Xám", "#808080", 349_000, 25),
                            new Variant("HOD-101-M-BLK", "M", "Đen", "#000000", 359_000, 18),
                            new Variant("HOD-101-L-BLK", "L", "Đen", "#000000", 359_000, 15)
                    )
            ),
            new SeedProduct(
                    "quan-tay-nam-cong-so",
                    "Quần tây nam công sở",
                    "Quần tây form slim, vải co giãn nhẹ, phù hợp môi trường công sở.",
                    459_000, "quan,tay,nam,cong-so", false,
                    "quan", "fashion-brand",
                    List.of("https://picsum.photos/seed/cartzilla-tay-1/800/1000"),
                    List.of(
                            new Variant("PNT-102-29-BLK", "29", "Đen", "#000000", 459_000, 15),
                            new Variant("PNT-102-30-BLK", "30", "Đen", "#000000", 459_000, 20),
                            new Variant("PNT-102-31-NVY", "31", "Xanh navy", "#1B263B", 459_000, 12)
                    )
            ),
            new SeedProduct(
                    "kinh-mat-thoi-trang-unisex",
                    "Kính mát thời trang unisex",
                    "Kính mát gọng nhựa, chống UV, phong cách trẻ trung.",
                    189_000, "phu-kien,kinh,mat,unisex", false,
                    "phu-kien", "local-supplier",
                    List.of("https://picsum.photos/seed/cartzilla-kinhmat-1/800/800"),
                    List.of(
                            new Variant("SGL-103-F-BLK", "F", "Đen", "#000000", 189_000, 40),
                            new Variant("SGL-103-F-BRN", "F", "Nâu", "#6B4423", 189_000, 25)
                    )
            ),
            new SeedProduct(
                    "giay-sneaker-trang-unisex",
                    "Giày sneaker trắng unisex",
                    "Sneaker da tổng hợp, đế cao su êm ái, dễ phối đồ.",
                    599_000, "phu-kien,giay,sneaker,unisex", true,
                    "phu-kien", "fashion-brand",
                    List.of(
                            "https://picsum.photos/seed/cartzilla-sneaker-1/800/1000",
                            "https://picsum.photos/seed/cartzilla-sneaker-2/800/1000"
                    ),
                    List.of(
                            new Variant("SNK-104-39-WHT", "39", "Trắng", "#FFFFFF", 599_000, 10),
                            new Variant("SNK-104-40-WHT", "40", "Trắng", "#FFFFFF", 599_000, 15),
                            new Variant("SNK-104-41-WHT", "41", "Trắng", "#FFFFFF", 599_000, 12),
                            new Variant("SNK-104-42-WHT", "42", "Trắng", "#FFFFFF", 599_000, 8)
                    )
            ),
            new SeedProduct(
                    "non-luoi-trai-unisex",
                    "Nón lưỡi trai unisex",
                    "Nón lưỡi trai vải kaki, có thể điều chỉnh size, unisex.",
                    99_000, "phu-kien,non,luoi-trai,unisex", false,
                    "phu-kien", "basic-collection",
                    List.of("https://picsum.photos/seed/cartzilla-nonluoitrai-1/800/800"),
                    List.of(
                            new Variant("CAP-105-F-BLK", "F", "Đen", "#000000", 99_000, 50),
                            new Variant("CAP-105-F-WHT", "F", "Trắng", "#FFFFFF", 99_000, 30),
                            new Variant("CAP-105-F-NVY", "F", "Xanh navy", "#1B263B", 99_000, 20)
                    )
            )
    );

    public static void main(String[] args) throws Exception {
        String dbHost = env("PRODUCT_DB_HOST", "localhost");
        String dbPort = env("PRODUCT_DB_PORT", "5433");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/cartzilla_product_db";
        String dbUser = env("DB_USER", "app");
        String dbPassword = env("DB_PASSWORD", "secret");

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", env("CLOUDINARY_CLOUD_NAME", "demo"),
                "api_key", env("CLOUDINARY_API_KEY", "123456789012345"),
                "api_secret", env("CLOUDINARY_API_SECRET", "demo_secret"),
                "secure", true
        ));

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            for (SeedProduct product : PRODUCTS) {
                seedOne(conn, cloudinary, product);
            }
        }
        System.out.println("Done.");
    }

    private static void seedOne(Connection conn, Cloudinary cloudinary, SeedProduct product) throws Exception {
        if (existsBySlug(conn, product.slug())) {
            System.out.println("Skip (already seeded): " + product.slug());
            return;
        }

        UUID categoryId = findIdBySlug(conn, "categories", product.categorySlug());
        UUID vendorId = findIdBySlug(conn, "vendors", product.vendorSlug());
        if (categoryId == null || vendorId == null) {
            System.out.println("Skip (missing category/vendor, run V2 migration first): " + product.slug());
            return;
        }

        UUID productId = insertProduct(conn, product, categoryId, vendorId);

        for (Variant v : product.variants()) {
            insertVariant(conn, productId, v);
        }

        int sortOrder = 0;
        for (String sourceUrl : product.sourceImageUrls()) {
            System.out.println("Uploading to Cloudinary: " + sourceUrl);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(sourceUrl, ObjectUtils.asMap(
                    "folder", "cartzilla/products"
            ));
            String secureUrl = (String) uploadResult.get("secure_url");
            insertImage(conn, productId, secureUrl, product.name(), sortOrder == 0, sortOrder);
            sortOrder++;
        }

        conn.commit();
        System.out.println("Seeded: " + product.slug());
    }

    private static boolean existsBySlug(Connection conn, String slug) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM products WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static UUID findIdBySlug(Connection conn, String table, String slug) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? (UUID) rs.getObject("id") : null;
            }
        }
    }

    private static UUID insertProduct(Connection conn, SeedProduct product, UUID categoryId, UUID vendorId) throws Exception {
        String sql = """
                INSERT INTO products (category_id, vendor_id, name, slug, description, base_price, tags, featured)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, categoryId);
            ps.setObject(2, vendorId);
            ps.setString(3, product.name());
            ps.setString(4, product.slug());
            ps.setString(5, product.description());
            ps.setLong(6, product.basePrice());
            ps.setString(7, product.tags());
            ps.setBoolean(8, product.featured());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return (UUID) rs.getObject("id");
            }
        }
    }

    private static void insertVariant(Connection conn, UUID productId, Variant v) throws Exception {
        String sql = """
                INSERT INTO product_variants (product_id, sku, size, color, color_hex, price, stock)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, productId);
            ps.setString(2, v.sku());
            ps.setString(3, v.size());
            ps.setString(4, v.color());
            ps.setString(5, v.colorHex());
            ps.setLong(6, v.price());
            ps.setInt(7, v.stock());
            ps.executeUpdate();
        }
    }

    private static void insertImage(Connection conn, UUID productId, String imageUrl, String altText,
                                     boolean isPrimary, int sortOrder) throws Exception {
        String sql = """
                INSERT INTO product_images (product_id, image_url, alt_text, is_primary, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, productId);
            ps.setString(2, imageUrl);
            ps.setString(3, altText);
            ps.setBoolean(4, isPrimary);
            ps.setInt(5, sortOrder);
            ps.executeUpdate();
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private ProductImageSeeder() {}
}
