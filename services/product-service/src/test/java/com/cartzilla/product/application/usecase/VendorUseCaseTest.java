package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.VendorCommand;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.VendorType;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorUseCaseTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private CreateVendorUseCase createVendorUseCase;

    @InjectMocks
    private UpdateVendorUseCase updateVendorUseCase;

    @InjectMocks
    private DeleteVendorUseCase deleteVendorUseCase;

    @InjectMocks
    private ListVendorsUseCase listVendorsUseCase;

    @Test
    @DisplayName("CreateVendorUseCase: Tạo vendor thành công")
    void createVendor_success() {
        VendorCommand.Create cmd = new VendorCommand.Create("Nike", "nike", "BRAND", "nike@nike.com", "0900000", "nike.com", "logo.png");
        when(vendorRepository.existsBySlug("nike")).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(i -> i.getArgument(0));

        Vendor vendor = createVendorUseCase.execute(cmd);

        assertNotNull(vendor);
        assertEquals("Nike", vendor.getName());
        assertEquals("nike", vendor.getSlug());
    }

    @Test
    @DisplayName("CreateVendorUseCase: Từ chối nếu slug đã tồn tại")
    void createVendor_duplicateSlug_throwsException() {
        VendorCommand.Create cmd = new VendorCommand.Create("Nike", "nike", "BRAND", null, null, null, null);
        when(vendorRepository.existsBySlug("nike")).thenReturn(true);

        assertThrows(BusinessException.class, () -> createVendorUseCase.execute(cmd));
    }

    @Test
    @DisplayName("UpdateVendorUseCase: Cập nhật vendor thành công")
    void updateVendor_success() {
        UUID id = UUID.randomUUID();
        Vendor existing = Vendor.create("Puma", "puma", VendorType.BRAND, null, null, null, null);
        when(vendorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(vendorRepository.existsBySlug("puma-official")).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(i -> i.getArgument(0));

        VendorCommand.Update cmd = new VendorCommand.Update("Puma Official", "puma-official", "BRAND", "puma@puma.com", "0900000", "puma.com", "logo.png", true);

        Vendor updated = updateVendorUseCase.execute(id, cmd);

        assertEquals("Puma Official", updated.getName());
        assertEquals("puma-official", updated.getSlug());
    }

    @Test
    @DisplayName("DeleteVendorUseCase: Soft delete vendor thành công")
    void deleteVendor_success() {
        UUID id = UUID.randomUUID();
        Vendor vendor = Vendor.create("Reebok", "reebok", VendorType.BRAND, null, null, null, null);
        when(vendorRepository.findById(id)).thenReturn(Optional.of(vendor));

        deleteVendorUseCase.execute(id);

        assertTrue(vendor.isDeleted());
        assertFalse(vendor.isActive());
        verify(vendorRepository).save(vendor);
    }

    @Test
    @DisplayName("ListVendorsUseCase: Lấy danh sách vendor active hoặc tất cả")
    void listVendors_success() {
        Vendor v1 = Vendor.create("V1", "v1", VendorType.BRAND, null, null, null, null);
        when(vendorRepository.findAllActive()).thenReturn(List.of(v1));

        List<Vendor> activeList = listVendorsUseCase.execute(false);
        assertEquals(1, activeList.size());
        verify(vendorRepository).findAllActive();
    }
}
