package com.swifteats.catalog;

import com.swifteats.catalog.controller.CatalogController;
import com.swifteats.catalog.dto.MenuItemDto;
import com.swifteats.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CatalogControllerTest {

    @Test
    public void testGetMenu() {
        MenuItemDto item = new MenuItemDto(1L, "Burger", 499);

        CatalogService service = Mockito.mock(CatalogService.class);
        when(service.getMenu(42L)).thenReturn(List.of(item));

        CatalogController controller = new CatalogController(service);
        ResponseEntity<List<MenuItemDto>> resp = controller.getMenu(42L);

        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("Burger");
    }
}
