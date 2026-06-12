package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AddressCommand;
import com.cartzilla.user.domain.entity.Address;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.AddressRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AddressUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryAddressRepository addressRepository = new InMemoryAddressRepository();

    @Test
    void createAddress_makesFirstAddressDefault() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase useCase = new CreateAddressUseCase(userRepository, addressRepository);

        Address address = useCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                false));

        assertTrue(address.isDefault());
        assertEquals(1, addressRepository.findByUserId(user.getId()).size());
    }

    @Test
    void setDefaultAddress_unsetsPreviousDefaultForSameUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase createUseCase = new CreateAddressUseCase(userRepository, addressRepository);
        SetDefaultAddressUseCase setDefaultUseCase = new SetDefaultAddressUseCase(userRepository, addressRepository);

        Address first = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));
        Address second = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0911111111",
                "2 Main St",
                "District 2",
                "Ho Chi Minh",
                false));

        Address selected = setDefaultUseCase.execute(user.getId(), second.getId());

        assertEquals(second.getId(), selected.getId());
        assertTrue(selected.isDefault());
        assertFalse(addressRepository.findById(first.getId()).orElseThrow().isDefault());
        assertEquals(1, addressRepository.findByUserId(user.getId()).stream()
                .filter(Address::isDefault)
                .count());
    }

    @Test
    void createAddress_withDefaultTrue_unsetsPreviousDefaultForSameUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase useCase = new CreateAddressUseCase(userRepository, addressRepository);

        Address first = useCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));
        Address second = useCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0911111111",
                "2 Main St",
                "District 2",
                "Ho Chi Minh",
                true));

        assertFalse(addressRepository.findById(first.getId()).orElseThrow().isDefault());
        assertTrue(addressRepository.findById(second.getId()).orElseThrow().isDefault());
        assertEquals(1, addressRepository.findByUserId(user.getId()).stream()
                .filter(Address::isDefault)
                .count());
    }

    @Test
    void updateAddress_withDefaultTrue_unsetsPreviousDefaultForSameUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase createUseCase = new CreateAddressUseCase(userRepository, addressRepository);
        UpdateAddressUseCase updateUseCase = new UpdateAddressUseCase(userRepository, addressRepository);

        Address first = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));
        Address second = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0911111111",
                "2 Main St",
                "District 2",
                "Ho Chi Minh",
                false));

        Address updated = updateUseCase.execute(user.getId(), second.getId(), new AddressCommand.Update(
                "Customer Updated",
                "0922222222",
                "2 Updated St",
                "District 3",
                "Ha Noi",
                true));

        assertTrue(updated.isDefault());
        assertEquals("Customer Updated", updated.getFullName());
        assertFalse(addressRepository.findById(first.getId()).orElseThrow().isDefault());
        assertEquals(1, addressRepository.findByUserId(user.getId()).stream()
                .filter(Address::isDefault)
                .count());
    }

    @Test
    void deleteAddress_rejectsDefaultAddressWhenAnotherAddressExists() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase createUseCase = new CreateAddressUseCase(userRepository, addressRepository);
        DeleteAddressUseCase deleteUseCase = new DeleteAddressUseCase(userRepository, addressRepository);

        Address first = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));
        createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0911111111",
                "2 Main St",
                "District 2",
                "Ho Chi Minh",
                false));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deleteUseCase.execute(user.getId(), first.getId()));

        assertTrue(ex.getMessage().contains("default"));
        assertEquals(2, addressRepository.findByUserId(user.getId()).size());
    }

    @Test
    void deleteAddress_allowsDeletingOnlyDefaultAddress() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        CreateAddressUseCase createUseCase = new CreateAddressUseCase(userRepository, addressRepository);
        DeleteAddressUseCase deleteUseCase = new DeleteAddressUseCase(userRepository, addressRepository);
        Address onlyAddress = createUseCase.execute(user.getId(), new AddressCommand.Create(
                "Customer",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));

        deleteUseCase.execute(user.getId(), onlyAddress.getId());

        assertTrue(addressRepository.findByUserId(user.getId()).isEmpty());
    }

    @Test
    void updateAddress_rejectsAddressOwnedByAnotherUser() {
        User owner = userRepository.save(User.createCustomer("owner@example.com", "hash", "Owner"));
        User other = userRepository.save(User.createCustomer("other@example.com", "hash", "Other"));
        CreateAddressUseCase createUseCase = new CreateAddressUseCase(userRepository, addressRepository);
        UpdateAddressUseCase updateUseCase = new UpdateAddressUseCase(userRepository, addressRepository);
        Address ownerAddress = createUseCase.execute(owner.getId(), new AddressCommand.Create(
                "Owner",
                "0900000000",
                "1 Main St",
                "District 1",
                "Ho Chi Minh",
                true));

        BusinessException ex = assertThrows(BusinessException.class, () -> updateUseCase.execute(
                other.getId(),
                ownerAddress.getId(),
                new AddressCommand.Update(
                        "Other",
                        "0911111111",
                        "2 Main St",
                        "District 2",
                        "Ha Noi",
                        false)));

        assertTrue(ex.getMessage().contains("Address not found"));
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();

        @Override
        public User save(User user) {
            assignIdIfMissing(user, "id");
            users.removeIf(existing -> existing.getId().equals(user.getId()));
            users.add(user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.stream().filter(user -> user.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }
    }

    private static class InMemoryAddressRepository implements AddressRepository {
        private final List<Address> addresses = new ArrayList<>();

        @Override
        public Address save(Address address) {
            assignIdIfMissing(address, "id");
            addresses.removeIf(existing -> existing.getId().equals(address.getId()));
            addresses.add(address);
            return address;
        }

        @Override
        public Optional<Address> findById(UUID id) {
            return addresses.stream().filter(address -> address.getId().equals(id)).findFirst();
        }

        @Override
        public List<Address> findByUserId(UUID userId) {
            return addresses.stream()
                    .filter(address -> address.getUserId().equals(userId))
                    .sorted(Comparator.comparing(Address::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }

        @Override
        public List<Address> findDefaultByUserId(UUID userId) {
            return findByUserId(userId).stream()
                    .filter(Address::isDefault)
                    .toList();
        }

        @Override
        public void delete(Address address) {
            if (!addresses.removeIf(existing -> existing.getId().equals(address.getId()))) {
                throw new BusinessException("Address not found");
            }
        }
    }

    private static void assignIdIfMissing(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.get(target) == null) {
                field.set(target, UUID.randomUUID());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
