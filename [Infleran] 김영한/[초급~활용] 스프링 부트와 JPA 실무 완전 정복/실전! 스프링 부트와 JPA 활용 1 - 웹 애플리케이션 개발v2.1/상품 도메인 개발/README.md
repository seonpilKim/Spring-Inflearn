# 상품 도메인 개발
- <b>구현 기능</b>
    - 상품 등록
    - 상품 목록 조회
    - 상품 수정
## 목차
- 상품 리포지토리 개발
- 상품 서비스 개발
___
## 상품 리포지토리 개발
### 상품 리포지토리 코드
```java
@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            em.merge(item);
        }
    }

    public Item findOne(Long itemId) {
        return em.find(Item.class, itemId);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }
}
```
- <b>기능 설명</b>
    - `save()`
        - `id`가 없으면, 신규로 보고 `persist()` 실행
        - `id`가 있으면, 이미 데이터베이스에 저장된 엔티티를 수정한다고 보고, `merge()`를 실행, 자세한 내용은 뒤에서 설명
## 상품 서비스 개발
### 상품 서비스 코드
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }

    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        Item item = itemRepository.findOne(itemId);
        item.update(name, price, stockQuantity);
    }
}
```