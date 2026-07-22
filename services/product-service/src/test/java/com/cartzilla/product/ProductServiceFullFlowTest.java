package com.cartzilla.product;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.application.command.CategoryCommand;
import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.application.command.VendorCommand;
import com.cartzilla.product.application.usecase.*;
import com.cartzilla.product.domain.entity.*;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.infrastructure.persistence.StockReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Full Flow Integration Test cho Product Service.
 * Kiểm tra toàn bộ luồng xử lý từ Vendor -> Category -> Product -> Variant -> Stock Reservation -> Soft Delete.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceFullFlowTest {

    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private StockReservationJpaRepository reservationRepository;

    private CreateVendorUseCase createVendorUseCase;
    private CreateCategoryUseCase createCategoryUseCase;
    private CreateProductUseCase createProductUseCase;
    private ManageVariantUseCase manageVariantUseCase;
    private DeleteProductUseCase deleteProductUseCase;
    private ReserveStockUseCase reserveStockUseCase;

    private final Map<UUID, Vendor> vendorMap = new HashMap<>();
    private final Map<UUID, Category> categoryMap = new HashMap<>();
    private final Map<UUID, Product> productMap = new HashMap<>();
    private final Map<String, ProductVariant> variantMap = new HashMap<>();
    private final Map<UUID, StockReservation> reservationMap = new HashMap<>();

    private static void setIdIfNull(Object entity) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            if (idField.get(entity) == null) {
                idField.set(entity, UUID.randomUUID());
            }
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() {
        vendorMap.clear();
        categoryMap.clear();
        productMap.clear();
        variantMap.clear();
        reservationMap.clear();

        // Setup mock repositories mimicking DB operations
        lenient().when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            setIdIfNull(v);
            vendorMap.put(v.getId(), v);
            return v;
        });
        lenient().when(vendorRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(vendorMap.get(inv.getArgument(0))));

        lenient().when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            setIdIfNull(c);
            categoryMap.put(c.getId(), c);
            return c;
        });
        lenient().when(categoryRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(categoryMap.get(inv.getArgument(0))));

        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            setIdIfNull(p);
            productMap.put(p.getId(), p);
            for (ProductVariant v : p.getVariants()) {
                variantMap.put(v.getSku(), v);
            }
            return p;
        });
        lenient().when(productRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(productMap.get(inv.getArgument(0))));

        lenient().when(variantRepository.findBySkuForUpdate(anyString())).thenAnswer(inv -> Optional.ofNullable(variantMap.get(inv.getArgument(0))));

        lenient().when(reservationRepository.findByOrderId(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(reservationMap.get(inv.getArgument(0))));
        lenient().when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
            StockReservation r = inv.getArgument(0);
            reservationMap.put(r.getOrderId(), r);
            return r;
        });
        lenient().doAnswer(inv -> {
            UUID orderId = inv.getArgument(0);
            reservationMap.putIfAbsent(orderId, new StockReservation(orderId));
            return null;
        }).when(reservationRepository).insertIfAbsent(any(UUID.class));

        // Initialize use cases
        createVendorUseCase = new CreateVendorUseCase(vendorRepository);
        createCategoryUseCase = new CreateCategoryUseCase(categoryRepository);
        createProductUseCase = new CreateProductUseCase(productRepository, categoryRepository, vendorRepository);
        manageVariantUseCase = new ManageVariantUseCase(productRepository);
        deleteProductUseCase = new DeleteProductUseCase(productRepository);
        reserveStockUseCase = new ReserveStockUseCase(variantRepository, reservationRepository);
    }

    @Test
    @DisplayName("FULL FLOW TEST: Vendor -> Category -> Product -> Variant & Image -> Stock Reservation -> Release -> Delete")
    void executeFullProductLifecycleFlow() {
        // 1. Tạo Vendor
        VendorCommand.Create vendorCmd = new VendorCommand.Create("Nike VietNam", "nike-vietnam", "BRAND", "info@nike.vn", "0901112223", "nike.vn", "logo.jpg");
        Vendor vendor = createVendorUseCase.execute(vendorCmd);
        assertNotNull(vendor.getId());
        assertTrue(vendor.isActive());

        // 2. Tạo Category
        CategoryCommand.Create categoryCmd = new CategoryCommand.Create("Giày Thể Thao", "giay-the-thao", null, null, 1);
        Category category = createCategoryUseCase.execute(categoryCmd);
        assertNotNull(category.getId());
        assertTrue(category.isActive());

        // 3. Tạo Product với 2 Variants và 1 Image
        ProductCommand.VariantData v1 = new ProductCommand.VariantData("AIR-MAX-40", "40", "Black", "#000000", new BigDecimal("2990000"), 10);
        ProductCommand.VariantData v2 = new ProductCommand.VariantData("AIR-MAX-41", "41", "White", "#FFFFFF", new BigDecimal("2990000"), 5);
        ProductCommand.ImageData img1 = new ProductCommand.ImageData("https://cdn/airmax.jpg", "Air Max 90", true, 0);

        ProductCommand.Create productCmd = new ProductCommand.Create(
                category.getId(), vendor.getId(), "Nike Air Max 90", "nike-air-max-90",
                "Giày chạy bộ cao cấp", new BigDecimal("2990000"), "running,nike,airmax",
                List.of(v1, v2), List.of(img1)
        );

        Product product = createProductUseCase.execute(productCmd);
        assertNotNull(product.getId());
        assertTrue(product.isSellable());
        assertEquals(2, product.getVariants().size());
        assertEquals(1, product.getImages().size());
        assertTrue(product.getImages().get(0).isPrimary());

        // 4. Giữ kho qua Saga Event (Order placement)
        UUID orderId = UUID.randomUUID();
        StockEvents.StockReserveEvent reserveEvent = new StockEvents.StockReserveEvent(
                orderId, List.of(
                new StockEvents.Item("AIR-MAX-40", 2),
                new StockEvents.Item("AIR-MAX-41", 1)
        ));

        String failedSku = reserveStockUseCase.reserve(reserveEvent);
        assertNull(failedSku, "Reserve stock thành công, không có SKU bị lỗi");

        // Kiểm tra tồn kho đã bị trừ
        assertEquals(8, variantMap.get("AIR-MAX-40").getStock());
        assertEquals(4, variantMap.get("AIR-MAX-41").getStock());

        // 5. Trừ thêm vượt tồn kho hiện tại -> phải từ chối (PA-07)
        UUID order2Id = UUID.randomUUID();
        StockEvents.StockReserveEvent overReserveEvent = new StockEvents.StockReserveEvent(
                order2Id, List.of(new StockEvents.Item("AIR-MAX-41", 10)));
        String rejectSku = reserveStockUseCase.reserve(overReserveEvent);
        assertEquals("AIR-MAX-41", rejectSku);
        assertEquals(4, variantMap.get("AIR-MAX-41").getStock(), "Stock không bị thay đổi khi từ chối");

        // 6. Hoàn lại tồn kho khi đơn hàng 1 bị hủy (Compensation Saga)
        StockEvents.StockReleaseEvent releaseEvent = new StockEvents.StockReleaseEvent(
                orderId, List.of(new StockEvents.Item("AIR-MAX-40", 2)));
        reserveStockUseCase.release(releaseEvent);
        assertEquals(10, variantMap.get("AIR-MAX-40").getStock(), "Tồn kho AIR-MAX-40 được hoàn trả lại 10");

        // 7. Thêm Variant mới vào Product (ManageVariantUseCase)
        ProductCommand.VariantData v3 = new ProductCommand.VariantData("AIR-MAX-42", "42", "Red", "#FF0000", new BigDecimal("3100000"), 15);
        Product updatedProduct = manageVariantUseCase.add(product.getId(), v3);
        assertEquals(3, updatedProduct.getVariants().size());

        // 8. Soft delete product và kiểm tra cascade
        deleteProductUseCase.execute(product.getId());
        Product deletedProduct = productMap.get(product.getId());
        assertTrue(deletedProduct.isDeleted());
        assertFalse(deletedProduct.isActive());
        assertFalse(deletedProduct.isSellable());
        assertTrue(deletedProduct.getVariants().stream().allMatch(ProductVariant::isDeleted));
        assertTrue(deletedProduct.getImages().stream().allMatch(ProductImage::isDeleted));
    }
}
