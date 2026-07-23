package com.cartzilla.product.tools;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone one-off fix script — KHÔNG phải Spring bean, không tự chạy khi app start.
 *
 * Thay toàn bộ ảnh hiện có của các sản phẩm bằng ảnh THẬT tìm theo từ khóa đúng loại sản phẩm
 * qua Unsplash Search API, sau đó upload thật lên Cloudinary (Cloudinary tải ảnh từ URL Unsplash
 * về và lưu lại). Mỗi sản phẩm có từ khóa tìm kiếm riêng (map cứng bên dưới) để đảm bảo ảnh
 * đúng loại quần áo/phụ kiện, không phải ảnh ngẫu nhiên hay chữ minh họa.
 *
 * Yêu cầu biến môi trường UNSPLASH_ACCESS_KEY (đăng ký free tại unsplash.com/developers).
 *
 * Chạy thủ công 1 lần, từ thư mục services/product-service:
 *   mvn compile
 *   mvn spring-boot:run -Dspring-boot.run.main-class=com.cartzilla.product.tools.ProductImageFixer
 */
public final class ProductImageFixer {

    private record ProductRow(UUID id, String name, String slug) {}

    private static final Pattern CLOUDINARY_PUBLIC_ID = Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    /** slug sản phẩm -> từ khóa tìm ảnh trên Unsplash (tiếng Anh để search chính xác hơn) */
    private static final Map<String, String> SEARCH_QUERY = Map.ofEntries(
            Map.entry("ao-thun-nam-basic-cotton", "mens white t-shirt"),
            Map.entry("ao-thun-nu-oversize", "womens oversized t-shirt"),
            Map.entry("ao-so-mi-nam-oxford", "mens oxford shirt"),
            Map.entry("ao-so-mi-nu-linen", "womens linen shirt"),
            Map.entry("quan-jean-nam-slim-fit", "mens slim fit jeans"),
            Map.entry("quan-short-kaki-nam", "mens khaki shorts"),
            Map.entry("quan-jogger-nu", "womens jogger pants"),
            Map.entry("quan-tay-nam-cong-so", "mens formal dress pants"),
            Map.entry("mu-bucket-unisex", "bucket hat"),
            Map.entry("non-luoi-trai-unisex", "baseball cap"),
            Map.entry("tui-tote-canvas", "canvas tote bag"),
            Map.entry("that-lung-da-nam", "mens leather belt"),
            Map.entry("ao-hoodie-unisex-ni-bong", "hoodie sweatshirt"),
            Map.entry("kinh-mat-thoi-trang-unisex", "fashion sunglasses"),
            Map.entry("giay-sneaker-trang-unisex", "white sneakers")
    );

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static String unsplashAccessKey;

    public static void main(String[] args) throws Exception {
        String dbHost = env("PRODUCT_DB_HOST", "localhost");
        String dbPort = env("PRODUCT_DB_PORT", "5433");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/cartzilla_product_db";
        String dbUser = env("DB_USER", "app");
        String dbPassword = env("DB_PASSWORD", "secret");
        unsplashAccessKey = env("UNSPLASH_ACCESS_KEY", null);
        if (unsplashAccessKey == null) {
            throw new IllegalStateException("Thiếu biến môi trường UNSPLASH_ACCESS_KEY");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", env("CLOUDINARY_CLOUD_NAME", "demo"),
                "api_key", env("CLOUDINARY_API_KEY", "123456789012345"),
                "api_secret", env("CLOUDINARY_API_SECRET", "demo_secret"),
                "secure", true
        ));

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            for (ProductRow product : fetchProducts(conn)) {
                try {
                    fixOne(conn, cloudinary, product);
                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("  (error) skip " + product.slug() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Done.");
    }

    private static List<ProductRow> fetchProducts(Connection conn) throws Exception {
        String sql = "SELECT id, name, slug FROM products WHERE slug <> 'test' ORDER BY name";
        List<ProductRow> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ProductRow((UUID) rs.getObject("id"), rs.getString("name"), rs.getString("slug")));
            }
        }
        return result;
    }

    private static void fixOne(Connection conn, Cloudinary cloudinary, ProductRow product) throws Exception {
        String query = SEARCH_QUERY.get(product.slug());
        if (query == null) {
            System.out.println("Skip (no search query mapped): " + product.slug());
            return;
        }

        List<UnsplashPhoto> photos = searchUnsplash(query, 2);
        if (photos.isEmpty()) {
            System.out.println("Skip (no Unsplash result): " + product.slug());
            return;
        }

        List<String> oldUrls = fetchImageUrls(conn, product.id());
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_images WHERE product_id = ?")) {
            ps.setObject(1, product.id());
            ps.executeUpdate();
        }
        for (String oldUrl : oldUrls) {
            destroyIfCloudinary(cloudinary, oldUrl);
        }

        int sortOrder = 0;
        for (UnsplashPhoto photo : photos) {
            System.out.println("  Uploading (" + query + "): " + photo.regularUrl());
            Map<?, ?> uploadResult = cloudinary.uploader().upload(photo.regularUrl(), ObjectUtils.asMap("folder", "cartzilla/products"));
            String secureUrl = (String) uploadResult.get("secure_url");
            insertImage(conn, product.id(), secureUrl, product.name(), sortOrder == 0, sortOrder);
            trackDownload(photo.downloadLocation());
            sortOrder++;
        }

        conn.commit();
        System.out.println("Fixed: " + product.name());
    }

    private record UnsplashPhoto(String regularUrl, String downloadLocation) {}

    private static List<UnsplashPhoto> searchUnsplash(String query, int count) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.unsplash.com/search/photos?query=" + encoded
                + "&per_page=" + count + "&orientation=portrait&content_filter=high";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Client-ID " + unsplashAccessKey)
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unsplash API " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = JSON.readTree(response.body());
        List<UnsplashPhoto> photos = new ArrayList<>();
        for (JsonNode result : root.path("results")) {
            String regularUrl = result.path("urls").path("regular").asText(null);
            String downloadLocation = result.path("links").path("download_location").asText(null);
            if (regularUrl != null) {
                photos.add(new UnsplashPhoto(regularUrl, downloadLocation));
            }
        }
        return photos;
    }

    private static void trackDownload(String downloadLocation) {
        if (downloadLocation == null) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(downloadLocation))
                    .header("Authorization", "Client-ID " + unsplashAccessKey)
                    .GET()
                    .build();
            HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // best-effort per Unsplash API guidelines, không chặn seed nếu lỗi
        }
    }

    private static List<String> fetchImageUrls(Connection conn, UUID productId) throws Exception {
        List<String> urls = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT image_url FROM product_images WHERE product_id = ?")) {
            ps.setObject(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) urls.add(rs.getString("image_url"));
            }
        }
        return urls;
    }

    private static void destroyIfCloudinary(Cloudinary cloudinary, String url) {
        if (url == null || !url.contains("res.cloudinary.com")) return;
        Matcher m = CLOUDINARY_PUBLIC_ID.matcher(url);
        if (!m.find()) return;
        try {
            cloudinary.uploader().destroy(m.group(1), ObjectUtils.emptyMap());
        } catch (Exception e) {
            System.out.println("  (warn) could not destroy old asset " + m.group(1) + ": " + e.getMessage());
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

    private ProductImageFixer() {}
}
