package hello.itemservice.domain.item;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ItemRepository {

    private static final Map<Long, Item> store = new HashMap<>(); // static
    private static long sequence = 0L; // static

    public Item save(Item item){
        item.setId(++sequence);
        store.put(item.getId(), item);
        return item;
    }

    public Item findById(Long id){
        return store.get(id);
    }

    public List<Item> findAll(){
        // ArrayList로 감싸서 반환하면, 외부에서 store의 값을 바꾸지 않도록 할 수 있으므로 안전하다.
        return new ArrayList<>(store.values());
    }

    /**
     * Item 클래스와 중복되는 데이터가 ItemDTO 클래스에 존재하더라도,
     * id를 제외한 다른 데이터만 update 하기 위해, 명확한 ItemDTO 클래스를 만듦.
     * 따라서 개발자는 findItem.getId() 같은 불필요한 호출을 시도하지 않게 된다.
     * 중복 vs 명확성 = 명확성 Win
     */
    public void update(Long itemId, ItemDTO updateDTO) {
        Item findItem = findById(itemId);

        findItem.setItemName(updateDTO.getItemName());
        findItem.setPrice(updateDTO.getPrice());
        findItem.setQuantity(updateDTO.getQuantity());
    }

    public void clearStore(){
        store.clear();
    }
}
