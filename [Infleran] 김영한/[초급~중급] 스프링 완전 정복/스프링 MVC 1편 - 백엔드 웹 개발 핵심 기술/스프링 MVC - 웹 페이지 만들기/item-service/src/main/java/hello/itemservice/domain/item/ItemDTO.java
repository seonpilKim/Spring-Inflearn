package hello.itemservice.domain.item;

import lombok.Data;

@Data
public class ItemDTO {

    private String itemName;
    private Integer price;
    private Integer quantity;

    public ItemDTO(){

    }

    public ItemDTO(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }

}
