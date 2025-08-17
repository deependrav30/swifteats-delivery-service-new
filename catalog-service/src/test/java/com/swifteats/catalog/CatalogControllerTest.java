package com.swifteats.catalog;

import com.swifteats.catalog.controller.CatalogController;
import com.swifteats.catalog.model.MenuItem;
import com.swifteats.catalog.repo.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CatalogControllerTest {

    @Test
    public void testGetMenu() {
        MenuItem item = new MenuItem();
        item.setId(1L);
        item.setRestaurantId(42L);
        item.setName("Burger");
        item.setPriceCents(499L);

        MenuItemRepository repo = Mockito.mock(MenuItemRepository.class);
        when(repo.findByRestaurantId(42L)).thenReturn(List.of(item));

        CatalogController controller = new CatalogController(repo);
        ResponseEntity<List<MenuItem>> resp = controller.getMenu(42L);

        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("Burger");
    }
}
