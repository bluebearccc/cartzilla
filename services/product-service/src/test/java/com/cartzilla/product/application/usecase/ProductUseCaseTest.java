package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.VendorType;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private CreateProductUseCase createProductUseCase;

    @InjectMocks
    private UpdateProductUseCase updateProductUseCase;

    @InjectMocks
    private DeleteProductUseCase deleteProductUseCase;

    @InjectMocks
    private GetProductUseCase getProductUseCase;

    @InjectMocks
    private ListProductsUseCase listProductsUseCase;

    @InjectMocks
    private ManageVariantUseCase manageVariantUseCase;

    @InjectMocks
    private ManageImageUseCase manageImageUseCase;

    private UUID categoryId;
    private UUID vendorId;
    private Category activeCategory;
    private Vendor activeVendor;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        activeCategory = Category.create("Category", "category", null, null, 0);
        activeVendor = Vendor.create("Vendor", "vendor", VendorType.BRAND, null, null, null, null);
    }

    @Test
    @DisplayName("CreateProductUseCase: Tạo sản phẩm mới kèm Variant & Image")
    void createProduct_success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(activeVendor));
        when(productRepository.existsBySlug("ao-polo")).thenReturn(false);
        when(productRepository.existsBySku("POLO-01")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductCommand.VariantData vData = new ProductCommand.VariantData("POLO-01", "L", "Trắng", "#FFFFFF", new BigDecimal("250000"), 20);
        ProductCommand.ImageData iData = new ProductCommand.ImageData("https://img/polo.jpg", "Polo image", true, 0);
        ProductCommand.Create cmd = new ProductCommand.Create(
                categoryId, vendorId, "Áo Polo", "ao-polo", "Mô tả", new BigDecimal("250000"), "polo,ao",
                List.of(vData), List.of(iData));

        Product product = createProductUseCase.execute(cmd);

        assertNotNull(product);
        assertEquals("Áo Polo", product.getName());
        assertEquals("ao-polo", product.getSlug());
        assertEquals(1, product.getVariants().size());
        assertEquals(1, product.getImages().size());
        assertTrue(product.getImages().get(0).isPrimary());
    }

    @Test
    @DisplayName("CreateProductUseCase: Từ chối nếu category bị inactive (P-01)")
    void createProduct_inactiveCategory_throwsException() {
        Category inactiveCategory = Category.create("Category", "category", null, null, 0);
        inactiveCategory.deactivate();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(inactiveCategory));

        ProductCommand.Create cmd = new ProductCommand.Create(
                categoryId, null, "Áo Polo", "ao-polo", null, new BigDecimal("250000"), null, null, null);

        assertThrows(BusinessException.class, () -> createProductUseCase.execute(cmd));
    }

    @Test
    @DisplayName("UpdateProductUseCase: Cập nhật sản phẩm thành công")
    void updateProduct_success() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(categoryId, "Tên cũ", "ten-cu", "Mô tả", new BigDecimal("100000"), vendorId, null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlug("ten-moi")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductCommand.Update cmd = new ProductCommand.Update(
                categoryId, vendorId, "Tên mới", "ten-moi", "Mô tả mới", new BigDecimal("150000"), "new-tag", true, true);

        Product updated = updateProductUseCase.execute(productId, cmd);

        assertEquals("Tên mới", updated.getName());
        assertEquals("ten-moi", updated.getSlug());
        assertEquals(0, new BigDecimal("150000").compareTo(updated.getBasePrice()));
    }

    @Test
    @DisplayName("DeleteProductUseCase: Soft delete sản phẩm")
    void deleteProduct_success() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(categoryId, "Sản phẩm", "san-pham", null, new BigDecimal("100000"), null, null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        deleteProductUseCase.execute(productId);

        assertTrue(product.isDeleted());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("ManageVariantUseCase: Thêm variant vào sản phẩm thành công")
    void addVariant_success() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(categoryId, "Giày", "giay", null, new BigDecimal("500000"), null, null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("GIAY-42")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductCommand.VariantData cmd = new ProductCommand.VariantData("GIAY-42", "42", "Đen", "#000000", new BigDecimal("500000"), 10);

        Product result = manageVariantUseCase.add(productId, cmd);

        assertEquals(1, result.getVariants().size());
        assertEquals("GIAY-42", result.getVariants().get(0).getSku());
    }

    @Test
    @DisplayName("ManageImageUseCase: Thêm và đặt primary image")
    void manageImage_success() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(categoryId, "Nón", "non", null, new BigDecimal("50000"), null, null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductCommand.ImageData img1 = new ProductCommand.ImageData("https://img1.jpg", "Img 1", true, 0);
        Product result = manageImageUseCase.add(productId, img1);

        assertEquals(1, result.getImages().size());
        assertTrue(result.getImages().get(0).isPrimary());
    }

    @Test
    @DisplayName("ListProductsUseCase: Tìm kiếm danh sách sản phẩm theo tiêu chí")
    void listProducts_success() {
        Product p1 = Product.create(categoryId, "P1", "p1", null, new BigDecimal("100000"), null, null);
        Page<Product> page = new PageImpl<>(List.of(p1));
        when(productRepository.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        Page<Product> result = listProductsUseCase.execute(ProductSearchCriteria.publicCatalog(
                null, null, "P1", null, null, null, null, null, null, null), 0, 10, "newest");

        assertEquals(1, result.getContent().size());
        assertEquals("P1", result.getContent().get(0).getName());
    }
}
