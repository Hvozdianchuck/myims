package com.ita.if103java.ims.dto;

import com.ita.if103java.ims.entity.Warehouse;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
@Component
public class SavedItemDto {
    @NotNull
    private Long id;
    @NotNull
    private Long itemId;
    @NotNull
    private ItemDto itemDto;
    @NotNull
    private int quantity;
    @NotNull
    private Long warehouseId;
    @NotNull
    private Warehouse warehouse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public ItemDto getItemDto() {
        return itemDto;
    }

    public void setItemDto(ItemDto itemDto) {
        this.itemDto = itemDto;
    }
}
